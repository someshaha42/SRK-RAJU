package com.framecut.app

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.framecut.ui.core.RecordedVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RecordingsRepository(private val context: Context) {

    private val appContext = context.applicationContext

    suspend fun loadRecordings(): List<RecordedVideoItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<RecordedVideoItem>()
        val seenNames = HashSet<String>()

        queryMediaStore(items, seenNames)
        queryLocalFallbackDir(items, seenNames)

        items.sortedByDescending { it.dateAddedSec }
    }

    private fun queryMediaStore(items: MutableList<RecordedVideoItem>, seenNames: MutableSet<String>) {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Video.Media.DATA)
            }
        }.toTypedArray()

        val selection: String
        val selectionArgs: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("%FrameCut%", "FrameCut_%")
        } else {
            @Suppress("DEPRECATION")
            selection = "${MediaStore.Video.Media.DATA} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("%FrameCut%", "FrameCut_%")
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            appContext.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val size = cursor.getLong(sizeCol)
                    if (size <= 0L) continue

                    val uri = ContentUris.withAppendedId(collection, id)
                    var width = cursor.getInt(widthCol)
                    var height = cursor.getInt(heightCol)
                    var duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateCol)

                    if ((width <= 0 || height <= 0 || duration <= 0L)) {
                        val meta = readMetadata(uri)
                        if (width <= 0) width = meta.first
                        if (height <= 0) height = meta.second
                        if (duration <= 0L) duration = meta.third
                    }

                    seenNames.add(name)
                    items.add(
                        RecordedVideoItem(
                            id = id,
                            displayName = name,
                            width = width,
                            height = height,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAddedSec = dateAdded,
                            uriString = uri.toString(),
                        ),
                    )
                }
            }
        }
    }

    private fun queryLocalFallbackDir(items: MutableList<RecordedVideoItem>, seenNames: MutableSet<String>) {
        val dirs = buildList {
            add(File(appContext.filesDir, "Recordings"))
            @Suppress("DEPRECATION")
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (moviesDir != null) {
                add(File(moviesDir, "FrameCut"))
            }
        }

        dirs.forEach { dir ->
            if (!dir.exists() || !dir.isDirectory) return@forEach
            dir.listFiles()?.forEach { file ->
                if (!file.isFile || !file.name.endsWith(".mp4", ignoreCase = true)) return@forEach
                if (file.length() <= 0L || seenNames.contains(file.name)) return@forEach

                val uri = runCatching {
                    FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileprovider",
                        file,
                    )
                }.getOrElse { Uri.fromFile(file) }

                val (w, h, d) = readMetadata(file.absolutePath)
                seenNames.add(file.name)
                items.add(
                    RecordedVideoItem(
                        id = file.absolutePath.hashCode().toLong() and 0x7FFFFFFFL,
                        displayName = file.name,
                        width = w,
                        height = h,
                        durationMs = d,
                        sizeBytes = file.length(),
                        dateAddedSec = file.lastModified() / 1000L,
                        uriString = uri.toString(),
                    ),
                )
            }
        }
    }

    suspend fun deleteRecording(item: RecordedVideoItem): Boolean = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(item.uriString) }.getOrNull() ?: return@withContext false
        if (uri.scheme == "content") {
            val deleted = runCatching {
                appContext.contentResolver.delete(uri, null, null) > 0
            }.getOrDefault(false)
            if (deleted) return@withContext true
        }

        val fallbackFile = File(File(appContext.filesDir, "Recordings"), item.displayName)
        if (fallbackFile.exists() && fallbackFile.delete()) {
            return@withContext true
        }
        false
    }

    fun buildPlayIntent(item: RecordedVideoItem): Intent? {
        val uri = runCatching { Uri.parse(item.uriString) }.getOrNull() ?: return null
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildShareIntent(item: RecordedVideoItem): Intent? {
        val uri = runCatching { Uri.parse(item.uriString) }.getOrNull() ?: return null
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, item.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(share, "Share Recording").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun readMetadata(uri: Uri): Triple<Int, Int, Long> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            Triple(w, h, d)
        } catch (_: Throwable) {
            Triple(0, 0, 0L)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun readMetadata(path: String): Triple<Int, Int, Long> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            Triple(w, h, d)
        } catch (_: Throwable) {
            Triple(0, 0, 0L)
        } finally {
            runCatching { retriever.release() }
        }
    }
}
