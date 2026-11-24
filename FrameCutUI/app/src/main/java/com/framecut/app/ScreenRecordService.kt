package com.framecut.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.framecut.ui.core.AudioSourceOption
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.RecordingMode
import com.framecut.ui.core.RecordingPhase
import com.framecut.ui.core.RecordingSessionState
import com.framecut.ui.core.RecordingSettings
import com.framecut.ui.core.RegionAnimationOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenRecordService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var captureEngine: ScreenCaptureEngine? = null
    private var timerJob: Job? = null
    private var overlayView: View? = null

    private var elapsedMillis: Long = 0L
    private var currentPhase: RecordingPhase = RecordingPhase.IDLE
    private var resolutionLabel: String = ""
    private var audioLabel: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
                val resultData = IntentCompat.getParcelableExtra(intent, EXTRA_RESULT_DATA, Intent::class.java)
                if (resultCode == Int.MIN_VALUE || resultData == null) {
                    RecordingController.setError("Screen capture permission was not granted.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startCaptureSession(resultCode, resultData)
            }
            ACTION_PAUSE -> pauseCapture()
            ACTION_RESUME -> resumeCapture()
            ACTION_STOP -> stopCaptureAndSave()
        }
        return START_NOT_STICKY
    }

    private fun startCaptureSession(resultCode: Int, resultData: Intent) {
        if (currentPhase == RecordingPhase.RECORDING || currentPhase == RecordingPhase.PAUSED) return

        val settings = SettingsRepository(this).load()
        elapsedMillis = 0L
        currentPhase = RecordingPhase.RECORDING
        audioLabel = if (settings.audioSource == AudioSourceOption.INTERNAL) "Internal Audio" else "Microphone"
        resolutionLabel = if (settings.recordingMode == RecordingMode.FULL_SCREEN) "Full Screen" else "Region"

        promoteToForeground()

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = runCatching {
            projectionManager.getMediaProjection(resultCode, resultData)
        }.getOrNull()

        if (mediaProjection == null) {
            RecordingController.setError("Failed to obtain MediaProjection from Android system.")
            stopForegroundAndFinish()
            return
        }

        val engine = ScreenCaptureEngine(
            context = this,
            settings = settings,
            mediaProjection = mediaProjection,
            onProjectionRevoked = {
                serviceScope.launch {
                    if (currentPhase == RecordingPhase.RECORDING || currentPhase == RecordingPhase.PAUSED) {
                        stopCaptureAndSave()
                    }
                }
            },
        )
        captureEngine = engine

        serviceScope.launch(Dispatchers.Default) {
            try {
                val (res, aud) = engine.start()
                withContext(Dispatchers.Main.immediate) {
                    resolutionLabel = res
                    audioLabel = aud
                    RecordingController.updateSession {
                        RecordingSessionState(
                            phase = RecordingPhase.RECORDING,
                            elapsedMillis = 0L,
                            activeResolutionLabel = res,
                            activeAudioLabel = aud,
                            lastSavedName = it.lastSavedName,
                            warningMessage = engine.getStartupWarning(),
                        )
                    }
                    if (settings.recordingMode == RecordingMode.REGION &&
                        settings.regionAnimation == RegionAnimationOption.SHOW
                    ) {
                        attachRegionBorderOverlay(settings.selectedRegion)
                    }
                    startTimerLoop()
                    updateNotification()
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main.immediate) {
                    RecordingController.setError("Capture failed: ${t.message ?: t::class.java.simpleName}")
                    removeRegionOverlay()
                    stopForegroundAndFinish()
                }
            }
        }
    }

    private fun pauseCapture() {
        if (currentPhase != RecordingPhase.RECORDING) return
        captureEngine?.pause()
        currentPhase = RecordingPhase.PAUSED
        timerJob?.cancel()
        timerJob = null
        RecordingController.setPhase(RecordingPhase.PAUSED)
        updateNotification()
    }

    private fun resumeCapture() {
        if (currentPhase != RecordingPhase.PAUSED) return
        captureEngine?.resume()
        currentPhase = RecordingPhase.RECORDING
        RecordingController.setPhase(RecordingPhase.RECORDING)
        startTimerLoop()
        updateNotification()
    }

    private fun stopCaptureAndSave() {
        if (currentPhase != RecordingPhase.RECORDING && currentPhase != RecordingPhase.PAUSED) {
            stopForegroundAndFinish()
            return
        }
        currentPhase = RecordingPhase.FINALIZING
        timerJob?.cancel()
        timerJob = null
        removeRegionOverlay()
        RecordingController.setPhase(RecordingPhase.FINALIZING)
        updateNotification()

        val engine = captureEngine
        captureEngine = null
        if (engine == null) {
            RecordingController.setPhase(RecordingPhase.IDLE)
            stopForegroundAndFinish()
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                val saved = engine.stopAndSave()
                withContext(Dispatchers.Main.immediate) {
                    currentPhase = RecordingPhase.IDLE
                    elapsedMillis = 0L
                    RecordingController.onRecordingSaved(saved.displayName)
                    saved.warningMessage?.let { RecordingController.setWarning(it) }
                    postSavedNotification(saved.displayName, saved.uri)
                    stopForegroundAndFinish()
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main.immediate) {
                    currentPhase = RecordingPhase.IDLE
                    elapsedMillis = 0L
                    RecordingController.setError("Failed to save recording: ${t.message ?: "Unknown error"}")
                    stopForegroundAndFinish()
                }
            }
        }
    }

    private fun promoteToForeground() {
        val hasMicPerm = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasMicPerm) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID_RECORDING,
            buildRecordingNotification(),
            fgsType,
        )
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var tickCount = 0
            while (isActive && currentPhase == RecordingPhase.RECORDING) {
                delay(250L)
                elapsedMillis += 250L
                RecordingController.setElapsedMillis(elapsedMillis)
                tickCount++
                if (tickCount % 4 == 0) {
                    updateNotification()
                }
            }
        }
    }

    private fun buildRecordingNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            REQ_OPEN_APP,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val formattedTime = formatTimer(elapsedMillis)
        val title = when (currentPhase) {
            RecordingPhase.RECORDING -> "Recording • $formattedTime"
            RecordingPhase.PAUSED -> "Paused • $formattedTime"
            RecordingPhase.FINALIZING -> "Finalizing MP4…"
            RecordingPhase.IDLE -> "FrameCut Ready"
        }
        val subtitle = listOf(resolutionLabel, audioLabel).filter { it.isNotBlank() }.joinToString(" · ")

        val builder = NotificationCompat.Builder(this, CHANNEL_RECORDING)
            .setSmallIcon(com.framecut.ui.assets.R.drawable.ic_stat_recording)
            .setContentTitle(title)
            .setContentText(if (subtitle.isNotBlank()) subtitle else "Tap to return to FrameCut")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (currentPhase == RecordingPhase.RECORDING) {
            val pausePi = serviceActionPendingIntent(ACTION_PAUSE, REQ_PAUSE)
            val stopPi = serviceActionPendingIntent(ACTION_STOP, REQ_STOP)
            builder.addAction(0, "Pause", pausePi)
            builder.addAction(0, "Stop", stopPi)
        } else if (currentPhase == RecordingPhase.PAUSED) {
            val resumePi = serviceActionPendingIntent(ACTION_RESUME, REQ_RESUME)
            val stopPi = serviceActionPendingIntent(ACTION_STOP, REQ_STOP)
            builder.addAction(0, "Resume", resumePi)
            builder.addAction(0, "Stop", stopPi)
        }

        return builder.build()
    }

    private fun postSavedNotification(displayName: String, uri: Uri?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = if (uri != null) {
            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            PendingIntent.getActivity(
                this,
                REQ_SAVED_PLAY,
                playIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            PendingIntent.getActivity(
                this,
                REQ_OPEN_APP,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_SAVED)
            .setSmallIcon(com.framecut.ui.assets.R.drawable.ic_stat_recording)
            .setContentTitle("Recording Saved")
            .setContentText("$displayName • Tap to play")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { manager.notify(NOTIFICATION_ID_SAVED, notification) }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_RECORDING, buildRecordingNotification())
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ScreenRecordService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val recChannel = NotificationChannel(
                CHANNEL_RECORDING,
                "Screen Recording Controls",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Persistent notification with Pause, Resume, and Stop controls during screen capture."
                setShowBadge(false)
            }
            val savedChannel = NotificationChannel(
                CHANNEL_SAVED,
                "Saved Recordings",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notifies when an MP4 screen recording finishes saving."
            }
            manager.createNotificationChannel(recChannel)
            manager.createNotificationChannel(savedChannel)
        }
    }

    private fun attachRegionBorderOverlay(region: NormalizedRegion) {
        if (!Settings.canDrawOverlays(this)) return
        removeRegionOverlay()

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

        val borderView = RegionOverlayBorderView(this, region.normalized())
        runCatching {
            wm.addView(borderView, params)
            overlayView = borderView
        }
    }

    private fun removeRegionOverlay() {
        val view = overlayView ?: return
        overlayView = null
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { wm.removeView(view) }
    }

    private fun stopForegroundAndFinish() {
        removeRegionOverlay()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        removeRegionOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    private class RegionOverlayBorderView(
        context: Context,
        private val region: NormalizedRegion,
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private var phase = 0f

        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(140, 0, 0, 0)
            strokeWidth = 4f * density
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#EF4444")
            strokeWidth = 2.5f * density
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val l = region.left * width
            val t = region.top * height
            val r = region.right * width
            val b = region.bottom * height

            canvas.drawRect(l, t, r, b, shadowPaint)
            borderPaint.pathEffect = DashPathEffect(floatArrayOf(10f * density, 6f * density), phase)
            canvas.drawRect(l, t, r, b, borderPaint)

            phase = (phase - 1.5f * density) % (16f * density)
            postInvalidateOnAnimation()
        }
    }

    companion object {
        const val ACTION_START = "com.framecut.app.action.START_RECORDING"
        const val ACTION_PAUSE = "com.framecut.app.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.framecut.app.action.RESUME_RECORDING"
        const val ACTION_STOP = "com.framecut.app.action.STOP_RECORDING"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val CHANNEL_RECORDING = "framecut_recording_channel"
        private const val CHANNEL_SAVED = "framecut_saved_channel"
        private const val NOTIFICATION_ID_RECORDING = 1001
        private const val NOTIFICATION_ID_SAVED = 1002

        private const val REQ_OPEN_APP = 200
        private const val REQ_PAUSE = 201
        private const val REQ_RESUME = 202
        private const val REQ_STOP = 203
        private const val REQ_SAVED_PLAY = 204

        fun createStartIntent(context: Context, resultCode: Int, data: Intent): Intent =
            Intent(context, ScreenRecordService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                this.action = action
            }
            runCatching { context.startService(intent) }
        }

        private fun formatTimer(elapsedMs: Long): String {
            val totalSeconds = (elapsedMs / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return "%02d:%02d:%02d".format(hours, minutes, seconds)
        }
    }
}
