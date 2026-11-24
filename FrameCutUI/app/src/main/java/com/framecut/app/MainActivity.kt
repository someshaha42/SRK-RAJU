package com.framecut.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.framecut.ui.core.CropMediaCategory
import com.framecut.ui.core.RecordedVideoItem
import com.framecut.ui.definitions.UiDefinitionLoader
import com.framecut.ui.screens.navigation.FrameCutNavHost
import com.framecut.ui.screens.navigation.FrameCutScreenCallbacks
import com.framecut.ui.theme.FrameCutTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FrameCutViewModel by viewModels()

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            viewModel.startRecordingWithProjection(result.resultCode, data)
        } else {
            viewModel.notifyInfo("Screen capture permission is required to start recording.")
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Audio permission denied — recording screen video only.",
                Toast.LENGTH_SHORT,
            ).show()
        }
        launchScreenCapturePrompt()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        proceedAudioAndProjectionFlow()
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        viewModel.refreshRecordings()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshOverlayPermission()
    }

    private val pickVisualMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.setSelectedCropUri(uri)
            Toast.makeText(this, "Media selected for cropping.", Toast.LENGTH_SHORT).show()
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.setSelectedCropUri(uri)
            Toast.makeText(this, "File loaded for cropping.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestInitialMediaReadPermission()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = uiState.settings.darkThemeOverride ?: systemDark
            var activePlayerItem by remember { mutableStateOf<RecordedVideoItem?>(null) }

            val loadedDefinitions = remember(applicationContext) {
                runCatching { UiDefinitionLoader.loadAll(applicationContext) }.getOrNull()
            }

            LaunchedEffect(loadedDefinitions) {
                // Offline definitions validated on startup.
            }

            val callbacks = remember(viewModel, systemDark) {
                FrameCutScreenCallbacks(
                    onToggleTheme = { viewModel.toggleTheme(systemDark) },
                    onStartRecordClick = { beginRecordingPermissionFlow() },
                    onPauseResumeClick = viewModel::pauseOrResumeRecording,
                    onStopClick = viewModel::stopRecording,
                    onSelectMode = viewModel::selectRecordingMode,
                    onSelectOrientationMode = viewModel::selectOrientationMode,
                    onSelectAudioSource = viewModel::selectAudioSource,
                    onSelectBitrate = viewModel::selectVideoBitrate,
                    onSelectRegionAnimation = viewModel::selectRegionAnimation,
                    onUpdateSelectedRegion = viewModel::updateSelectedRegion,
                    onRequestOverlayPermission = { openOverlayPermissionSettings() },
                    onDismissMessages = viewModel::dismissMessages,
                    onSaveCustomBitrateMbps = viewModel::saveCustomVideoBitrateMbps,
                    onResetCustomBitrate = viewModel::resetCustomVideoBitrate,
                    onSelectBitrateMode = viewModel::selectBitrateMode,
                    onSelectColorDepth = viewModel::selectColorDepth,
                    onSelectVideoFps = viewModel::selectVideoFps,
                    onSelectGopInterval = viewModel::selectGopInterval,
                    onSelectColorRange = viewModel::selectColorRange,
                    onSelectAudioFormat = viewModel::selectAudioFormat,
                    onSaveCustomFps = viewModel::saveCustomVideoFps,
                    onResetCustomFps = viewModel::resetCustomVideoFps,
                    onSaveCustomGopSeconds = viewModel::saveCustomGopSeconds,
                    onResetCustomGop = viewModel::resetCustomGop,
                    onSaveCustomAudio = viewModel::saveCustomAudio,
                    onResetCustomAudio = viewModel::resetCustomAudio,
                    onPlayRecording = { item -> activePlayerItem = item },
                    onShareRecording = { item -> launchShareIntent(item) },
                    onDeleteRecording = viewModel::deleteRecording,
                    onPickCropMedia = { category ->
                        val type = when (category) {
                            CropMediaCategory.PHOTO -> ActivityResultContracts.PickVisualMedia.ImageOnly
                            CropMediaCategory.VIDEO -> ActivityResultContracts.PickVisualMedia.VideoOnly
                            CropMediaCategory.ALL -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        }
                        pickVisualMediaLauncher.launch(PickVisualMediaRequest(type))
                    },
                    onBrowseCropFile = {
                        openDocumentLauncher.launch(arrayOf("image/*", "video/*"))
                    },
                    onExportCropped = { region, quality, preserveMeta ->
                        viewModel.exportCroppedMedia(region, quality, preserveMeta) { message ->
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }

            FrameCutTheme(darkTheme = isDarkTheme) {
                FrameCutNavHost(
                    uiState = uiState,
                    isDarkTheme = isDarkTheme,
                    callbacks = callbacks,
                )

                activePlayerItem?.let { item ->
                    VideoPlayerDialog(
                        item = item,
                        onDismiss = { activePlayerItem = null },
                        onOpenExternal = {
                            activePlayerItem = null
                            launchExternalPlayer(item)
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshOverlayPermission()
        viewModel.refreshRecordings()
    }

    private fun beginRecordingPermissionFlow() {
        if (!viewModel.uiState.value.isIdle) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        proceedAudioAndProjectionFlow()
    }

    private fun proceedAudioAndProjectionFlow() {
        val audioGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!audioGranted) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        launchScreenCapturePrompt()
    }

    private fun launchScreenCapturePrompt() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        runCatching { overlayPermissionLauncher.launch(intent) }
            .onFailure { viewModel.notifyInfo("Overlay permission settings not available on this device.") }
    }

    private fun requestInitialMediaReadPermission() {
        val perms = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            storagePermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun launchExternalPlayer(item: RecordedVideoItem) {
        val intent = viewModel.recordingsRepository.buildPlayIntent(item)
        if (intent == null) {
            Toast.makeText(this, "Video file URI is unavailable.", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching { startActivity(intent) }
            .onFailure { Toast.makeText(this, "No external video player app found.", Toast.LENGTH_SHORT).show() }
    }

    private fun launchShareIntent(item: RecordedVideoItem) {
        val intent = viewModel.recordingsRepository.buildShareIntent(item)
        if (intent == null) {
            Toast.makeText(this, "Video file URI is unavailable.", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching { startActivity(intent) }
            .onFailure { Toast.makeText(this, "Unable to share video.", Toast.LENGTH_SHORT).show() }
    }
}
