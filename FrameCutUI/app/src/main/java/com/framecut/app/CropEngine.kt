package com.framecut.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.framecut.ui.core.NormalizedRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CropExportResult(
    val displayName: String,
    val outputUri: Uri?,
    val summary: String,
)

class CropEngine(private val context: Context) {

    private val appContext = context.applicationContext

    suspend fun exportMedia(
        sourceUri: Uri,
        region: NormalizedRegion,
        qualityPercent: Int,
        preserveMetadata: Boolean,
    ): CropExportResult = withContext(Dispatchers.IO) {
        val mime = appContext.contentResolver.getType(sourceUri).orEmpty()
        if (mime.startsWith("video/", ignoreCase = true)) {
            exportVideo(sourceUri, region)
        } else {
            exportImage(sourceUri, region, qualityPercent, preserveMetadata)
        }
    }

    private fun exportImage(
        sourceUri: Uri,
        region: NormalizedRegion,
        qualityPercent: Int,
        preserveMetadata: Boolean,
    ): CropExportResult {
        val rawBytes = appContext.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: error("Unable to read selected image file.")

        var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: error("Unsupported image format.")

        if (preserveMetadata && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val orientation = runCatching {
                rawBytes.inputStream().use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                }
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            bitmap = applyExifRotation(bitmap, orientation)
        }

        val norm = region.normalized()
        val cropBitmap = if (norm.isFullFrame()) {
            bitmap
        } else {
            val left = (norm.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val top = (norm.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            val width = ((norm.right - norm.left) * bitmap.width).toInt().coerceIn(1, bitmap.width - left)
            val height = ((norm.bottom - norm.top) * bitmap.height).toInt().coerceIn(1, bitmap.height - top)
            Bitmap.createBitmap(bitmap, left, top, width, height)
        }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "FrameCut_Crop_$stamp.jpg"
        val outUri = saveImageToPictures(cropBitmap, displayName, qualityPercent.coerceIn(25, 100))
        if (cropBitmap !== bitmap) bitmap.recycle()

        return CropExportResult(
            displayName = displayName,
            outputUri = outUri,
            summary = "Saved cropped image ($displayName) to Pictures/FrameCut.",
        )
    }

    private fun exportVideo(sourceUri: Uri, region: NormalizedRegion): CropExportResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "FrameCut_Video_$stamp.mp4"
        val tempOut = File(appContext.cacheDir, displayName)

        val extractor = MediaExtractor()
        extractor.setDataSource(appContext, sourceUri, null)

        val muxer = MediaMuxer(tempOut.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackMap = HashMap<Int, Int>()
        var maxBufSize = 1024 * 1024

        try {
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    trackMap[i] = muxer.addTrack(format)
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        maxBufSize = maxOf(maxBufSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                }
            }

            muxer.start()
            val buffer = ByteBuffer.allocate(maxBufSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                val srcTrack = extractor.sampleTrackIndex
                val dstTrack = trackMap[srcTrack]
                if (dstTrack != null) {
                    muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                }
                extractor.advance()
            }
            muxer.stop()
        } finally {
            runCatching { muxer.release() }
            runCatching { extractor.release() }
        }

        val outUri = saveVideoToMovies(tempOut, displayName)
        runCatching { tempOut.delete() }
        RecordingController.notifyRecordingsChanged()

        val label = if (region.isFullFrame()) "1:1 stream copy" else "video export"
        return CropExportResult(
            displayName = displayName,
            outputUri = outUri,
            summary = "Saved $label ($displayName) to Movies/FrameCut.",
        )
    }

    private fun saveImageToPictures(bitmap: Bitmap, displayName: String, quality: Int): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FrameCut")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                appContext.contentResolver.update(uri, values, null, null)
                return uri
            }
        }
        val dir = File(appContext.filesDir, "Crops").apply { mkdirs() }
        val file = File(dir, displayName)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out) }
        return Uri.fromFile(file)
    }

    private fun saveVideoToMovies(sourceFile: File, displayName: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/FrameCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = appContext.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                appContext.contentResolver.update(uri, values, null, null)
                return uri
            }
        }
        val dir = File(appContext.filesDir, "Recordings").apply { mkdirs() }
        val file = File(dir, displayName)
        FileInputStream(sourceFile).use { input -> FileOutputStream(file).use { out -> input.copyTo(out) } }
        return Uri.fromFile(file)
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
