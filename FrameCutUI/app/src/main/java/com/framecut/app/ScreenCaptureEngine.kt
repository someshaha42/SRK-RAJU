package com.framecut.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.framecut.ui.core.AudioSourceOption
import com.framecut.ui.core.BitrateModeOption
import com.framecut.ui.core.ColorDepthOption
import com.framecut.ui.core.ColorRangeOption
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.OrientationMode
import com.framecut.ui.core.RecordingMode
import com.framecut.ui.core.RecordingSettings
import com.framecut.ui.core.RegionGeometry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SavedRecordingResult(
    val displayName: String,
    val uri: Uri?,
    val width: Int,
    val height: Int,
    val warningMessage: String? = null,
)

class ScreenCaptureEngine(
    private val context: Context,
    private val settings: RecordingSettings,
    private val mediaProjection: MediaProjection,
    private val onProjectionRevoked: () -> Unit,
) {
    private val appContext = context.applicationContext

    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var glRenderer: GlRegionCropRenderer? = null
    private var audioRecord: AudioRecord? = null

    private var glThread: HandlerThread? = null
    private var glHandler: Handler? = null
    private var videoDrainThread: Thread? = null
    private var audioCaptureThread: Thread? = null
    private var audioDrainThread: Thread? = null

    private val muxerLock = Object()
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var hasAudioTrack = false
    private var muxerStarted = false

    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val pendingGlFrames = AtomicInteger(0)

    private var startElapsedNs = 0L
    private var pauseStartNs = 0L
    private val pausedAccumulatedNs = AtomicLong(0L)
    private var renderedVideoFrames = 0L
    private var lastDrawnFrameNs = 0L
    private var lastVideoPtsUs = -1L
    private var lastAudioPtsUs = -1L

    private val targetFps: Int = (settings.customVideoFps ?: settings.videoFps.fps).coerceIn(15, 120)
    private val minFrameIntervalNs: Long = ((1_000_000_000L / targetFps) * 80L) / 100L

    private var tempOutputFile: File? = null
    private var targetWidth = 1080
    private var targetHeight = 1920
    private var displayWidth = 1080
    private var displayHeight = 1920
    private var displayDpi = 320
    private var startupWarning: String? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            if (isRunning.get()) {
                onProjectionRevoked()
            }
        }
    }

    fun start(): Pair<String, String> {
        val (screenW, screenH, dpi) = resolveDisplayMetrics()
        displayDpi = dpi
        if (settings.orientationMode == OrientationMode.PORTRAIT) {
            displayWidth = min(screenW, screenH)
            displayHeight = max(screenW, screenH)
        } else {
            displayWidth = max(screenW, screenH)
            displayHeight = min(screenW, screenH)
        }

        val region = if (settings.recordingMode == RecordingMode.FULL_SCREEN) {
            NormalizedRegion(0f, 0f, 1f, 1f)
        } else {
            settings.selectedRegion.normalized()
        }

        val rawPixelRegion = RegionGeometry.toPixelRegion(region, displayWidth, displayHeight)
        val rawW = if (settings.recordingMode == RecordingMode.FULL_SCREEN) displayWidth else rawPixelRegion.width
        val rawH = if (settings.recordingMode == RecordingMode.FULL_SCREEN) displayHeight else rawPixelRegion.height

        val configuredVideo = configureVideoEncoder(rawW, rawH)
        videoEncoder = configuredVideo.codec
        targetWidth = configuredVideo.width
        targetHeight = configuredVideo.height

        val audioReady = setupAudioPipeline()
        hasAudioTrack = audioReady

        val tempFile = File(appContext.cacheDir, "framecut_rec_${System.currentTimeMillis()}.mp4")
        tempOutputFile = tempFile
        muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val encoderInputSurface = configuredVideo.inputSurface
        val thread = HandlerThread("FrameCutGLThread", Process.THREAD_PRIORITY_DISPLAY).also { it.start() }
        glThread = thread
        val handler = Handler(thread.looper)
        glHandler = handler

        isRunning.set(true)
        isPaused.set(false)
        startElapsedNs = SystemClock.elapsedRealtimeNanos()
        pausedAccumulatedNs.set(0L)
        lastDrawnFrameNs = 0L

        val glReadyLatch = CountDownLatch(1)
        var virtualInputSurface: Surface? = null
        var glError: Throwable? = null

        handler.post {
            try {
                val renderer = GlRegionCropRenderer(
                    encoderInputSurface = encoderInputSurface,
                    displayWidth = displayWidth,
                    displayHeight = displayHeight,
                    region = region,
                    onFrameSignal = { scheduleGlDraw() },
                )
                virtualInputSurface = renderer.prepare(callbackHandler = handler)
                renderer.setViewport(targetWidth, targetHeight)
                glRenderer = renderer
            } catch (t: Throwable) {
                glError = t
            } finally {
                glReadyLatch.countDown()
            }
        }
        glReadyLatch.await(5, TimeUnit.SECONDS)
        glError?.let { throw it }

        val vdSurface = virtualInputSurface ?: error("GL input surface creation failed")

        mediaProjection.registerCallback(projectionCallback, handler)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "FrameCutCapture",
            displayWidth,
            displayHeight,
            displayDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            vdSurface,
            null,
            handler,
        )

        videoEncoder?.start()
        videoDrainThread = Thread(::drainVideoEncoderLoop, "FrameCutVideoDrain").apply { start() }

        if (hasAudioTrack) {
            audioEncoder?.start()
            audioRecord?.startRecording()
            audioCaptureThread = Thread(::captureAudioLoop, "FrameCutAudioCapture").apply { start() }
            audioDrainThread = Thread(::drainAudioEncoderLoop, "FrameCutAudioDrain").apply { start() }
        }

        val audioDesc = if (hasAudioTrack) {
            if (settings.audioSource == AudioSourceOption.INTERNAL) "Internal Audio" else "Microphone"
        } else {
            "Video Only"
        }
        return "${targetWidth}×${targetHeight}" to audioDesc
    }

    fun getStartupWarning(): String? = startupWarning

    fun pause() {
        if (!isRunning.get() || isPaused.get()) return
        pauseStartNs = SystemClock.elapsedRealtimeNanos()
        isPaused.set(true)
    }

    fun resume() {
        if (!isRunning.get() || !isPaused.get()) return
        val delta = (SystemClock.elapsedRealtimeNanos() - pauseStartNs).coerceAtLeast(0L)
        pausedAccumulatedNs.addAndGet(delta)
        isPaused.set(false)
        scheduleGlDraw()
    }

    fun stopAndSave(): SavedRecordingResult {
        isRunning.set(false)
        isPaused.set(false)

        // Ensure at least one frame is rendered so MP4 track is never empty
        val flushLatch = CountDownLatch(1)
        glHandler?.post {
            runCatching {
                if (renderedVideoFrames == 0L) {
                    glRenderer?.drawFrame(currentAdjustedPtsNs(), updateTex = false)
                    renderedVideoFrames++
                }
                videoEncoder?.signalEndOfInputStream()
            }
            flushLatch.countDown()
        }
        flushLatch.await(2, TimeUnit.SECONDS)

        runCatching { audioCaptureThread?.join(2500) }
        runCatching { audioDrainThread?.join(2500) }
        runCatching { videoDrainThread?.join(3500) }

        releaseResources()

        val sourceFile = tempOutputFile
        if (sourceFile == null || !sourceFile.exists() || sourceFile.length() <= 0L) {
            error("No video data was written by hardware encoder.")
        }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "FrameCut_$stamp.mp4"
        val savedUri = saveMp4ToMovies(sourceFile, displayName)
        runCatching { sourceFile.delete() }

        return SavedRecordingResult(
            displayName = displayName,
            uri = savedUri,
            width = targetWidth,
            height = targetHeight,
            warningMessage = startupWarning,
        )
    }

    private fun scheduleGlDraw() {
        val handler = glHandler ?: return
        if (pendingGlFrames.getAndIncrement() == 0) {
            handler.post {
                while (pendingGlFrames.getAndSet(0) > 0) {
                    val renderer = glRenderer ?: break
                    runCatching {
                        val nowNs = currentAdjustedPtsNs()
                        val shouldPaceSkip = renderedVideoFrames > 0L && (nowNs - lastDrawnFrameNs) < minFrameIntervalNs
                        if (!isRunning.get() || isPaused.get() || shouldPaceSkip) {
                            renderer.updateTexImageOnly()
                        } else {
                            lastDrawnFrameNs = nowNs
                            renderer.drawFrame(nowNs, updateTex = true)
                            renderedVideoFrames++
                        }
                    }
                }
            }
        }
    }

    private fun currentAdjustedPtsNs(): Long {
        val raw = SystemClock.elapsedRealtimeNanos() - startElapsedNs - pausedAccumulatedNs.get()
        return raw.coerceAtLeast(1_000L)
    }

    private fun drainVideoEncoderLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE)
        val encoder = videoEncoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        var eosSeen = false

        while (!eosSeen) {
            val index = try {
                encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
            } catch (_: Throwable) {
                break
            }

            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!isRunning.get() && renderedVideoFrames == 0L) {
                        // Continue waiting briefly until signalEndOfInputStream EOS arrives
                    }
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    synchronized(muxerLock) {
                        if (videoTrackIndex < 0) {
                            videoTrackIndex = muxer?.addTrack(encoder.outputFormat) ?: -1
                            maybeStartMuxerLocked()
                        }
                    }
                }
                index >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(index)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && encodedData != null) {
                        synchronized(muxerLock) {
                            if (waitForMuxerStartLocked() && videoTrackIndex >= 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                if (bufferInfo.presentationTimeUs <= lastVideoPtsUs) {
                                    bufferInfo.presentationTimeUs = lastVideoPtsUs + 1_000L
                                }
                                lastVideoPtsUs = bufferInfo.presentationTimeUs
                                muxer?.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            }
                        }
                    }
                    encoder.releaseOutputBuffer(index, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosSeen = true
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupAudioPipeline(): Boolean {
        val hasMicPerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPerm) {
            startupWarning = "Audio permission not granted — recording video only."
            return false
        }

        val sampleRate = (settings.customAudioSampleRateHz ?: 48_000).coerceIn(8_000, 96_000)
        val audioBitrate = (settings.customAudioBitrateBps ?: 128_000).coerceIn(32_000, 320_000)
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)

        val record: AudioRecord? = if (settings.audioSource == AudioSourceOption.INTERNAL) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build()
                    val format = AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                    AudioRecord.Builder()
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(minBuf * 2)
                        .setAudioPlaybackCaptureConfig(config)
                        .build()
                }.getOrNull()
            } else {
                startupWarning = "Internal audio requires Android 10+; switched to Microphone."
                null
            }
        } else {
            null
        }

        val finalRecord = record ?: runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
        }.getOrNull()

        if (finalRecord == null || finalRecord.state != AudioRecord.STATE_INITIALIZED) {
            finalRecord?.release()
            startupWarning = "Audio recorder unavailable — recording video only."
            return false
        }

        val aacMime = MediaFormat.MIMETYPE_AUDIO_AAC
        val audioFormat = MediaFormat.createAudioFormat(aacMime, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBuf * 2)
        }

        val codec = runCatching {
            MediaCodec.createEncoderByType(aacMime).apply {
                configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
        }.getOrNull()

        if (codec == null) {
            finalRecord.release()
            startupWarning = "AAC encoder unavailable — recording video only."
            return false
        }

        audioRecord = finalRecord
        audioEncoder = codec
        return true
    }

    private fun captureAudioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val record = audioRecord ?: return
        val encoder = audioEncoder ?: return
        val pcmBuffer = ByteArray(4096)

        while (isRunning.get()) {
            val bytesRead = record.read(pcmBuffer, 0, pcmBuffer.size)
            if (bytesRead <= 0 || isPaused.get()) {
                continue
            }

            val inputIndex = try {
                encoder.dequeueInputBuffer(10_000L)
            } catch (_: Throwable) {
                break
            }
            if (inputIndex >= 0) {
                val inputBuf = encoder.getInputBuffer(inputIndex) ?: continue
                inputBuf.clear()
                val toCopy = min(bytesRead, inputBuf.remaining())
                inputBuf.put(pcmBuffer, 0, toCopy)
                val ptsUs = currentAdjustedPtsNs() / 1_000L
                encoder.queueInputBuffer(inputIndex, 0, toCopy, ptsUs, 0)
            }
        }

        runCatching {
            val eosIndex = encoder.dequeueInputBuffer(10_000L)
            if (eosIndex >= 0) {
                encoder.queueInputBuffer(
                    eosIndex,
                    0,
                    0,
                    currentAdjustedPtsNs() / 1_000L,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                )
            }
        }
    }

    private fun drainAudioEncoderLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE)
        val encoder = audioEncoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        var eosSeen = false

        while (!eosSeen) {
            val index = try {
                encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
            } catch (_: Throwable) {
                break
            }

            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    synchronized(muxerLock) {
                        if (audioTrackIndex < 0) {
                            audioTrackIndex = muxer?.addTrack(encoder.outputFormat) ?: -1
                            maybeStartMuxerLocked()
                        }
                    }
                }
                index >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(index)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && encodedData != null) {
                        synchronized(muxerLock) {
                            if (waitForMuxerStartLocked() && audioTrackIndex >= 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                if (bufferInfo.presentationTimeUs <= lastAudioPtsUs) {
                                    bufferInfo.presentationTimeUs = lastAudioPtsUs + 1_000L
                                }
                                lastAudioPtsUs = bufferInfo.presentationTimeUs
                                muxer?.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                            }
                        }
                    }
                    encoder.releaseOutputBuffer(index, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosSeen = true
                    }
                }
            }
        }
    }

    private fun maybeStartMuxerLocked() {
        val videoReady = videoTrackIndex >= 0
        val audioReady = !hasAudioTrack || audioTrackIndex >= 0
        if (!muxerStarted && videoReady && audioReady) {
            muxer?.start()
            muxerStarted = true
            muxerLock.notifyAll()
        }
    }

    private fun waitForMuxerStartLocked(): Boolean {
        val deadline = System.currentTimeMillis() + 3_000L
        while (!muxerStarted && isRunning.get()) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0L) break
            muxerLock.wait(remaining)
        }
        return muxerStarted
    }

    private data class ConfiguredVideo(
        val codec: MediaCodec,
        val inputSurface: Surface,
        val width: Int,
        val height: Int,
    )

    private fun configureVideoEncoder(rawW: Int, rawH: Int): ConfiguredVideo {
        val desiredFps = settings.customVideoFps ?: settings.videoFps.fps
        val desiredBitrateBps = ((settings.customVideoBitrateMbps ?: settings.videoBitrate.mbps.toLong()) * 1_000_000L)
            .coerceIn(1_000_000L, 120_000_000L)
            .toInt()
        val desiredGopSec = (settings.customGopSeconds ?: settings.gopInterval.secondsFloat).coerceAtLeast(0.5f)
        val iFrameIntervalSec = max(1, desiredGopSec.roundToInt())

        val candidateMimes = listOf(MediaFormat.MIMETYPE_VIDEO_HEVC, MediaFormat.MIMETYPE_VIDEO_AVC)
        var lastError: Throwable? = null

        for (mime in candidateMimes) {
            val encoderInfo = findHardwareEncoder(mime) ?: continue
            val caps = runCatching { encoderInfo.getCapabilitiesForType(mime).videoCapabilities }.getOrNull()

            var w = ((rawW / 16) * 16).coerceAtLeast(128)
            var h = ((rawH / 16) * 16).coerceAtLeast(128)
            if (caps != null) {
                val maxW = caps.supportedWidths.upper
                val maxH = caps.supportedHeights.upper
                if (w > maxW || h > maxH) {
                    val scale = min(maxW.toFloat() / w, maxH.toFloat() / h)
                    w = (((w * scale).toInt() / 16) * 16).coerceAtLeast(128)
                    h = (((h * scale).toInt() / 16) * 16).coerceAtLeast(128)
                }
                if (!caps.isSizeSupported(w, h)) {
                    w = (((min(w, maxW)) / 16) * 16).coerceAtLeast(128)
                    h = (((min(h, caps.getSupportedHeightsFor(w).upper)) / 16) * 16).coerceAtLeast(128)
                }
            }

            val clampedBitrate = caps?.bitrateRange?.clamp(desiredBitrateBps) ?: desiredBitrateBps
            val clampedFps = caps?.supportedFrameRates?.clamp(desiredFps) ?: desiredFps

            val format = MediaFormat.createVideoFormat(mime, w, h).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, clampedBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, clampedFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSec)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709)
                    setInteger(
                        MediaFormat.KEY_COLOR_RANGE,
                        if (settings.colorRange == ColorRangeOption.FULL) {
                            MediaFormat.COLOR_RANGE_FULL
                        } else {
                            MediaFormat.COLOR_RANGE_LIMITED
                        },
                    )
                }
                val bitrateMode = if (settings.bitrateMode == BitrateModeOption.CQ) {
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ
                } else {
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                }
                setInteger(MediaFormat.KEY_BITRATE_MODE, bitrateMode)
                if (mime == MediaFormat.MIMETYPE_VIDEO_HEVC && settings.colorDepth == ColorDepthOption.BIT_10) {
                    val codecCaps = runCatching { encoderInfo.getCapabilitiesForType(mime) }.getOrNull()
                    val supportsMain10 = codecCaps?.profileLevels?.any {
                        it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
                    } == true
                    if (supportsMain10) {
                        setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10)
                    } else if (startupWarning == null) {
                        startupWarning = "10-Bit HEVC profile unavailable on this GPU; recorded in 8-Bit."
                    }
                }
            }

            try {
                val codec = MediaCodec.createByCodecName(encoderInfo.name)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val inputSurface = codec.createInputSurface()
                if (mime != MediaFormat.MIMETYPE_VIDEO_HEVC && startupWarning == null) {
                    startupWarning = "HEVC unsupported at this resolution; encoded with H.264/AVC."
                }
                return ConfiguredVideo(codec, inputSurface, w, h)
            } catch (t: Throwable) {
                lastError = t
            }
        }

        // Last-resort generic AVC fallback
        val fallbackW = ((rawW / 16) * 16).coerceIn(320, 1080)
        val fallbackH = ((rawH / 16) * 16).coerceIn(320, 1920)
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val fallbackFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, fallbackW, fallbackH).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        try {
            codec.configure(fallbackFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            return ConfiguredVideo(codec, codec.createInputSurface(), fallbackW, fallbackH)
        } catch (t: Throwable) {
            codec.release()
            throw lastError ?: t
        }
    }

    private fun findHardwareEncoder(mime: String): MediaCodecInfo? {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
        }
    }

    private fun saveMp4ToMovies(sourceFile: File, displayName: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/FrameCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = appContext.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    appContext.contentResolver.update(uri, values, null, null)
                    return uri
                } catch (_: Throwable) {
                    runCatching { appContext.contentResolver.delete(uri, null, null) }
                }
            }
        }

        // Legacy API < 29 or fallback storage
        @Suppress("DEPRECATION")
        val moviesPublic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val publicDir = File(moviesPublic, "FrameCut")
        val targetDir = if (publicDir.exists() || publicDir.mkdirs()) {
            publicDir
        } else {
            File(appContext.filesDir, "Recordings").apply { mkdirs() }
        }
        val destFile = File(targetDir, displayName)
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        MediaScannerConnection.scanFile(appContext, arrayOf(destFile.absolutePath), arrayOf("video/mp4"), null)
        return Uri.fromFile(destFile)
    }

    private fun resolveDisplayMetrics(): Triple<Int, Int, Int> {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = appContext.resources.displayMetrics
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            return Triple(bounds.width().coerceAtLeast(480), bounds.height().coerceAtLeast(480), dm.densityDpi)
        }
        @Suppress("DEPRECATION")
        val realMetrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(realMetrics)
        return Triple(realMetrics.widthPixels.coerceAtLeast(480), realMetrics.heightPixels.coerceAtLeast(480), realMetrics.densityDpi)
    }

    private fun releaseResources() {
        runCatching { mediaProjection.unregisterCallback(projectionCallback) }
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null

        val releaseGlLatch = CountDownLatch(1)
        glHandler?.post {
            runCatching { glRenderer?.release() }
            glRenderer = null
            releaseGlLatch.countDown()
        }
        releaseGlLatch.await(2, TimeUnit.SECONDS)

        runCatching { glThread?.quitSafely() }
        glThread = null
        glHandler = null

        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null

        runCatching {
            audioEncoder?.stop()
            audioEncoder?.release()
        }
        audioEncoder = null

        runCatching {
            videoEncoder?.stop()
            videoEncoder?.release()
        }
        videoEncoder = null

        runCatching {
            if (muxerStarted) muxer?.stop()
            muxer?.release()
        }
        muxer = null
        muxerStarted = false

        runCatching { mediaProjection.stop() }
    }
}
