package com.framecut.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.Surface
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.RegionGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

data class LoadedCropMedia(
    val uri: Uri,
    val displayName: String,
    val isVideo: Boolean,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val previewBitmap: Bitmap?,
)

data class CropExportResult(
    val displayName: String,
    val outputUri: Uri?,
    val summary: String,
)

class CropEngine(private val context: Context) {

    private val appContext = context.applicationContext

    suspend fun loadMediaPreview(sourceUri: Uri): LoadedCropMedia = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(sourceUri)
        val mime = appContext.contentResolver.getType(sourceUri).orEmpty()
        val isVideo = mime.startsWith("video/", ignoreCase = true) ||
            displayName.endsWith(".mp4", ignoreCase = true) ||
            displayName.endsWith(".mkv", ignoreCase = true) ||
            displayName.endsWith(".mov", ignoreCase = true)

        if (isVideo) {
            loadVideoPreview(sourceUri, displayName)
        } else {
            loadImagePreview(sourceUri, displayName)
        }
    }

    suspend fun exportMedia(
        sourceUri: Uri,
        region: NormalizedRegion,
        qualityPercent: Int,
        preserveMetadata: Boolean,
    ): CropExportResult = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(sourceUri)
        val mime = appContext.contentResolver.getType(sourceUri).orEmpty()
        val isVideo = mime.startsWith("video/", ignoreCase = true) ||
            displayName.endsWith(".mp4", ignoreCase = true) ||
            displayName.endsWith(".mkv", ignoreCase = true) ||
            displayName.endsWith(".mov", ignoreCase = true)

        if (isVideo) {
            exportVideo(sourceUri, region, qualityPercent)
        } else {
            exportImage(sourceUri, region, qualityPercent, preserveMetadata)
        }
    }

    private fun loadImagePreview(sourceUri: Uri, displayName: String): LoadedCropMedia {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOpts)
        }
        val rawW = boundsOpts.outWidth.coerceAtLeast(1)
        val rawH = boundsOpts.outHeight.coerceAtLeast(1)

        val orientation = readImageExifOrientation(sourceUri)
        val rotated = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270

        val sampleSize = calculateInSampleSize(rawW, rawH, MAX_PREVIEW_DIM, MAX_PREVIEW_DIM)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var preview = appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOpts)
        }
        if (preview != null && orientation != ExifInterface.ORIENTATION_NORMAL) {
            preview = applyExifRotation(preview, orientation)
        }

        val visualW = if (rotated) rawH else rawW
        val visualH = if (rotated) rawW else rawH
        return LoadedCropMedia(
            uri = sourceUri,
            displayName = displayName,
            isVideo = false,
            width = visualW,
            height = visualH,
            durationMs = 0L,
            previewBitmap = preview,
        )
    }

    private fun loadVideoPreview(sourceUri: Uri, displayName: String): LoadedCropMedia {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, sourceUri)
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

            val isRotated = rotation == 90 || rotation == 270
            val visualW = if (isRotated) rawH else rawW
            val visualH = if (isRotated) rawW else rawH

            val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val scale = min(1f, MAX_PREVIEW_DIM.toFloat() / max(visualW, visualH).coerceAtLeast(1))
                val targetW = (visualW * scale).toInt().coerceAtLeast(64)
                val targetH = (visualH * scale).toInt().coerceAtLeast(64)
                retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetW, targetH)
                    ?: retriever.getFrameAtTime(0L)
            } else {
                retriever.getFrameAtTime(0L)
            }

            LoadedCropMedia(
                uri = sourceUri,
                displayName = displayName,
                isVideo = true,
                width = visualW.coerceAtLeast(1),
                height = visualH.coerceAtLeast(1),
                durationMs = durationMs,
                previewBitmap = frame,
            )
        } finally {
            runCatching { retriever.release() }
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

        val sourceExif: ExifInterface? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { rawBytes.inputStream().use { ExifInterface(it) } }.getOrNull()
        } else {
            null
        }
        val orientation = sourceExif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        ) ?: ExifInterface.ORIENTATION_NORMAL

        // Always normalize pixel orientation so crop matches visual preview
        if (orientation != ExifInterface.ORIENTATION_NORMAL) {
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
        val tempJpg = File(appContext.cacheDir, displayName)
        FileOutputStream(tempJpg).use { out ->
            cropBitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent.coerceIn(25, 100), out)
        }
        if (cropBitmap !== bitmap) bitmap.recycle()

        if (preserveMetadata && sourceExif != null) {
            copyCameraExifTags(sourceExif, tempJpg)
        }

        val outUri = saveImageFileToPictures(tempJpg, displayName)
        runCatching { tempJpg.delete() }

        return CropExportResult(
            displayName = displayName,
            outputUri = outUri,
            summary = "Saved cropped image ($displayName) to Pictures/FrameCut.",
        )
    }

    private fun exportVideo(
        sourceUri: Uri,
        region: NormalizedRegion,
        qualityPercent: Int,
    ): CropExportResult {
        val norm = region.normalized()
        return if (norm.isFullFrame()) {
            exportVideoStreamCopy(sourceUri)
        } else {
            exportVideoCropped(sourceUri, norm, qualityPercent)
        }
    }

    private fun exportVideoStreamCopy(sourceUri: Uri): CropExportResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "FrameCut_Copy_$stamp.mp4"
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
            val bufferInfo = MediaCodec.BufferInfo()

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

        return CropExportResult(
            displayName = displayName,
            outputUri = outUri,
            summary = "Saved 1:1 original stream copy ($displayName) to Movies/FrameCut.",
        )
    }

    private fun exportVideoCropped(
        sourceUri: Uri,
        region: NormalizedRegion,
        qualityPercent: Int,
    ): CropExportResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "FrameCut_Crop_$stamp.mp4"
        val tempOut = File(appContext.cacheDir, displayName)

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var glRenderer: GlRegionCropRenderer? = null
        var glThread: HandlerThread? = null

        try {
            videoExtractor.setDataSource(appContext, sourceUri, null)
            audioExtractor.setDataSource(appContext, sourceUri, null)

            var srcVideoTrack = -1
            var srcVideoFormat: MediaFormat? = null
            var srcAudioTrack = -1
            var srcAudioFormat: MediaFormat? = null

            for (i in 0 until videoExtractor.trackCount) {
                val fmt = videoExtractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (srcVideoTrack < 0 && mime.startsWith("video/")) {
                    srcVideoTrack = i
                    srcVideoFormat = fmt
                } else if (srcAudioTrack < 0 && mime.startsWith("audio/")) {
                    srcAudioTrack = i
                    srcAudioFormat = fmt
                }
            }
            val videoFormat = srcVideoFormat ?: error("No video track found in selected file.")
            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_VIDEO_AVC
            videoExtractor.selectTrack(srcVideoTrack)

            val rawW = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val rawH = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val rotation = if (videoFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                videoFormat.getInteger(MediaFormat.KEY_ROTATION)
            } else {
                0
            }
            val orientedW = if (rotation == 90 || rotation == 270) rawH else rawW
            val orientedH = if (rotation == 90 || rotation == 270) rawW else rawH

            val cropPx = RegionGeometry.toPixelRegion(region, orientedW, orientedH, evenAlign = true)
            val srcFps = if (videoFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE).coerceIn(15, 120)
            } else {
                30
            }

            val encConfig = configureCropVideoEncoder(
                targetW = cropPx.width,
                targetH = cropPx.height,
                fps = srcFps,
                qualityPercent = qualityPercent,
            )
            encoder = encConfig.codec
            val encoderInputSurface = encConfig.inputSurface

            val glSyncLock = Object()
            var frameAvailable = false

            val thread = HandlerThread("FrameCutCropGL", Process.THREAD_PRIORITY_DISPLAY).also { it.start() }
            glThread = thread
            val glHandler = Handler(thread.looper)

            val glReadyLatch = CountDownLatch(1)
            var decoderSurface: Surface? = null
            var glInitError: Throwable? = null

            glHandler.post {
                try {
                    val renderer = GlRegionCropRenderer(
                        encoderInputSurface = encoderInputSurface,
                        displayWidth = orientedW,
                        displayHeight = orientedH,
                        region = region,
                        onFrameSignal = {
                            synchronized(glSyncLock) {
                                frameAvailable = true
                                glSyncLock.notifyAll()
                            }
                        },
                    )
                    decoderSurface = renderer.prepare(callbackHandler = glHandler)
                    renderer.setViewport(encConfig.width, encConfig.height)
                    glRenderer = renderer
                } catch (t: Throwable) {
                    glInitError = t
                } finally {
                    glReadyLatch.countDown()
                }
            }
            glReadyLatch.await(5, TimeUnit.SECONDS)
            glInitError?.let { throw it }
            val decodeOutSurface = decoderSurface ?: error("Failed to initialize OpenGL crop surface.")

            decoder = MediaCodec.createDecoderByType(videoMime).apply {
                configure(videoFormat, decodeOutSurface, null, 0)
                start()
            }
            encoder.start()

            muxer = MediaMuxer(tempOut.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var dstVideoTrack = -1
            var dstAudioTrack = -1
            if (srcAudioTrack >= 0 && srcAudioFormat != null) {
                audioExtractor.selectTrack(srcAudioTrack)
                dstAudioTrack = muxer.addTrack(srcAudioFormat)
            }

            val audioBuf = ByteBuffer.allocate(256 * 1024)
            val audioInfo = MediaCodec.BufferInfo()
            var audioDone = srcAudioTrack < 0

            val decInfo = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var decoderDone = false
            var encoderDone = false
            var lastVideoPtsUs = -1L
            var lastAudioPtsUs = -1L

            fun flushEncoderStep(timeoutUs: Long) {
                while (true) {
                    val outIdx = encoder.dequeueOutputBuffer(encInfo, timeoutUs)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (dstVideoTrack < 0) {
                                dstVideoTrack = muxer.addTrack(encoder.outputFormat)
                                if (!muxerStarted) {
                                    muxer.start()
                                    muxerStarted = true
                                }
                            }
                        }
                        outIdx >= 0 -> {
                            val encoded = encoder.getOutputBuffer(outIdx)
                            if ((encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                encInfo.size = 0
                            }
                            if (encInfo.size > 0 && encoded != null && muxerStarted && dstVideoTrack >= 0) {
                                encoded.position(encInfo.offset)
                                encoded.limit(encInfo.offset + encInfo.size)
                                if (encInfo.presentationTimeUs <= lastVideoPtsUs) {
                                    encInfo.presentationTimeUs = lastVideoPtsUs + 1_000L
                                }
                                lastVideoPtsUs = encInfo.presentationTimeUs
                                muxer.writeSampleData(dstVideoTrack, encoded, encInfo)
                            }
                            encoder.releaseOutputBuffer(outIdx, false)
                            if ((encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                encoderDone = true
                                break
                            }
                        }
                    }
                }
            }

            fun writeAudioUpTo(targetPtsUs: Long) {
                if (audioDone || !muxerStarted || dstAudioTrack < 0) return
                while (true) {
                    val sampleTime = audioExtractor.sampleTime
                    if (sampleTime < 0L) {
                        audioDone = true
                        break
                    }
                    if (sampleTime > targetPtsUs) break
                    audioInfo.offset = 0
                    audioInfo.size = audioExtractor.readSampleData(audioBuf, 0)
                    if (audioInfo.size < 0) {
                        audioDone = true
                        break
                    }
                    audioInfo.presentationTimeUs = if (sampleTime <= lastAudioPtsUs) lastAudioPtsUs + 1_000L else sampleTime
                    lastAudioPtsUs = audioInfo.presentationTimeUs
                    audioInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(dstAudioTrack, audioBuf, audioInfo)
                    audioExtractor.advance()
                }
            }

            while (!encoderDone) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sampleSize = videoExtractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(
                                    inIdx,
                                    0,
                                    sampleSize,
                                    videoExtractor.sampleTime,
                                    videoExtractor.sampleFlags,
                                )
                                videoExtractor.advance()
                            }
                        }
                    }
                }

                if (!decoderDone) {
                    val decOutIdx = decoder.dequeueOutputBuffer(decInfo, 10_000L)
                    when {
                        decOutIdx >= 0 -> {
                            val render = decInfo.size > 0
                            if (render) {
                                synchronized(glSyncLock) { frameAvailable = false }
                            }
                            decoder.releaseOutputBuffer(decOutIdx, render)
                            if (render) {
                                var gotFrame = false
                                synchronized(glSyncLock) {
                                    val deadline = System.currentTimeMillis() + 500L
                                    while (!frameAvailable) {
                                        val waitMs = deadline - System.currentTimeMillis()
                                        if (waitMs <= 0L) break
                                        glSyncLock.wait(waitMs)
                                    }
                                    gotFrame = frameAvailable
                                }
                                if (gotFrame) {
                                    val drawLatch = CountDownLatch(1)
                                    val ptsNs = decInfo.presentationTimeUs * 1_000L
                                    glHandler.post {
                                        runCatching { glRenderer?.drawFrame(ptsNs, updateTex = true) }
                                        drawLatch.countDown()
                                    }
                                    drawLatch.await(2, TimeUnit.SECONDS)
                                    flushEncoderStep(0L)
                                    writeAudioUpTo(decInfo.presentationTimeUs)
                                }
                            }
                            if ((decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                decoderDone = true
                                encoder.signalEndOfInputStream()
                            }
                        }
                    }
                } else {
                    flushEncoderStep(10_000L)
                }
            }

            // Flush any trailing audio samples after last video frame
            writeAudioUpTo(Long.MAX_VALUE)

            if (muxerStarted) {
                muxer.stop()
            }
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching {
                val latch = CountDownLatch(1)
                glThread?.looper?.let { looper ->
                    Handler(looper).post {
                        runCatching { glRenderer?.release() }
                        latch.countDown()
                    }
                    latch.await(1, TimeUnit.SECONDS)
                }
            }
            runCatching { glThread?.quitSafely() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { muxer?.release() }
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
        }

        if (!tempOut.exists() || tempOut.length() <= 0L) {
            error("Cropped video export produced an empty file.")
        }

        val outUri = saveVideoToMovies(tempOut, displayName)
        runCatching { tempOut.delete() }
        RecordingController.notifyRecordingsChanged()

        return CropExportResult(
            displayName = displayName,
            outputUri = outUri,
            summary = "Saved cropped video ($displayName) to Movies/FrameCut.",
        )
    }

    private data class CropEncoderConfig(
        val codec: MediaCodec,
        val inputSurface: Surface,
        val width: Int,
        val height: Int,
    )

    private fun configureCropVideoEncoder(
        targetW: Int,
        targetH: Int,
        fps: Int,
        qualityPercent: Int,
    ): CropEncoderConfig {
        val mimes = listOf(MediaFormat.MIMETYPE_VIDEO_AVC, MediaFormat.MIMETYPE_VIDEO_HEVC)
        val qFactor = (qualityPercent.coerceIn(25, 100) / 100f)
        var lastError: Throwable? = null

        for (mime in mimes) {
            val info = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull {
                it.isEncoder && it.supportedTypes.any { type -> type.equals(mime, ignoreCase = true) }
            } ?: continue
            val caps = runCatching { info.getCapabilitiesForType(mime).videoCapabilities }.getOrNull()

            var w = ((targetW / 16) * 16).coerceAtLeast(128)
            var h = ((targetH / 16) * 16).coerceAtLeast(128)
            if (caps != null) {
                val maxW = caps.supportedWidths.upper
                val maxH = caps.supportedHeights.upper
                if (w > maxW || h > maxH) {
                    val scale = min(maxW.toFloat() / w, maxH.toFloat() / h)
                    w = (((w * scale).toInt() / 16) * 16).coerceAtLeast(128)
                    h = (((h * scale).toInt() / 16) * 16).coerceAtLeast(128)
                }
            }
            val rawBps = ((w * h).toLong() * fps * 0.16 * qFactor).toLong().coerceIn(1_500_000L, 40_000_000L).toInt()
            val clampedBps = caps?.bitrateRange?.clamp(rawBps) ?: rawBps
            val clampedFps = caps?.supportedFrameRates?.clamp(fps) ?: fps

            val fmt = MediaFormat.createVideoFormat(mime, w, h).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, clampedBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, clampedFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            try {
                val codec = MediaCodec.createByCodecName(info.name)
                codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                return CropEncoderConfig(codec, codec.createInputSurface(), w, h)
            } catch (t: Throwable) {
                lastError = t
            }
        }

        val fallbackW = ((targetW / 16) * 16).coerceIn(128, 1080)
        val fallbackH = ((targetH / 16) * 16).coerceIn(128, 1920)
        val fallback = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, fallbackW, fallbackH).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        try {
            fallback.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            return CropEncoderConfig(fallback, fallback.createInputSurface(), fallbackW, fallbackH)
        } catch (t: Throwable) {
            fallback.release()
            throw lastError ?: t
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Selected Media"
    }

    private fun readImageExifOrientation(uri: Uri): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return ExifInterface.ORIENTATION_NORMAL
        return runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun copyCameraExifTags(sourceExif: ExifInterface, destFile: File) {
        runCatching {
            val destExif = ExifInterface(destFile.absolutePath)
            val tags = listOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_ISO_SPEED_RATINGS,
                ExifInterface.TAG_FOCAL_LENGTH,
            )
            tags.forEach { tag ->
                sourceExif.getAttribute(tag)?.let { value ->
                    destExif.setAttribute(tag, value)
                }
            }
            destExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            destExif.saveAttributes()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (height > reqH || width > reqW) {
            val halfH = height / 2
            val halfW = width / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun saveImageFileToPictures(sourceFile: File, displayName: String): Uri? {
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
                    FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                appContext.contentResolver.update(uri, values, null, null)
                return uri
            }
        }
        @Suppress("DEPRECATION")
        val picturesPublic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val publicDir = File(picturesPublic, "FrameCut")
        val dir = if (publicDir.exists() || publicDir.mkdirs()) {
            publicDir
        } else {
            File(appContext.filesDir, "Crops").apply { mkdirs() }
        }
        val file = File(dir, displayName)
        FileInputStream(sourceFile).use { input -> FileOutputStream(file).use { out -> input.copyTo(out) } }
        MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
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
        @Suppress("DEPRECATION")
        val moviesPublic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val publicDir = File(moviesPublic, "FrameCut")
        val targetDir = if (publicDir.exists() || publicDir.mkdirs()) {
            publicDir
        } else {
            File(appContext.filesDir, "Recordings").apply { mkdirs() }
        }
        val file = File(targetDir, displayName)
        FileInputStream(sourceFile).use { input -> FileOutputStream(file).use { out -> input.copyTo(out) } }
        MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath), arrayOf("video/mp4"), null)
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

    companion object {
        private const val MAX_PREVIEW_DIM = 1080
    }
}
