package com.framecut.ui.screens.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.framecut.ui.core.AudioFormatOption
import com.framecut.ui.core.AudioSourceOption
import com.framecut.ui.core.BitrateModeOption
import com.framecut.ui.core.ColorDepthOption
import com.framecut.ui.core.ColorRangeOption
import com.framecut.ui.core.CropMediaCategory
import com.framecut.ui.core.FrameCutUiState
import com.framecut.ui.core.GopIntervalOption
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.OrientationMode
import com.framecut.ui.core.RecordedVideoItem
import com.framecut.ui.core.RecordingMode
import com.framecut.ui.core.RegionAnimationOption
import com.framecut.ui.core.UiConstants
import com.framecut.ui.core.VideoBitrateOption
import com.framecut.ui.core.VideoFpsOption
import com.framecut.ui.screens.components.RegionSelectorOverlay
import com.framecut.ui.screens.crop.CropToolScreen
import com.framecut.ui.screens.main.MainScreen
import com.framecut.ui.screens.recordings.RecordingsScreen
import com.framecut.ui.screens.settings.SettingsScreen

data class FrameCutScreenCallbacks(
    val onToggleTheme: () -> Unit = {},
    val onStartRecordClick: () -> Unit = {},
    val onPauseResumeClick: () -> Unit = {},
    val onStopClick: () -> Unit = {},
    val onSelectMode: (RecordingMode) -> Unit = {},
    val onSelectOrientationMode: (OrientationMode) -> Unit = {},
    val onSelectAudioSource: (AudioSourceOption) -> Unit = {},
    val onSelectBitrate: (VideoBitrateOption) -> Unit = {},
    val onSelectRegionAnimation: (RegionAnimationOption) -> Unit = {},
    val onUpdateSelectedRegion: (NormalizedRegion) -> Unit = {},
    val onRequestOverlayPermission: () -> Unit = {},
    val onDismissMessages: () -> Unit = {},
    val onSaveCustomBitrateMbps: (Long) -> Unit = {},
    val onResetCustomBitrate: () -> Unit = {},
    val onSelectBitrateMode: (BitrateModeOption) -> Unit = {},
    val onSelectColorDepth: (ColorDepthOption) -> Unit = {},
    val onSelectVideoFps: (VideoFpsOption) -> Unit = {},
    val onSelectGopInterval: (GopIntervalOption) -> Unit = {},
    val onSelectColorRange: (ColorRangeOption) -> Unit = {},
    val onSelectAudioFormat: (AudioFormatOption) -> Unit = {},
    val onSaveCustomFps: (Int) -> Unit = {},
    val onResetCustomFps: () -> Unit = {},
    val onSaveCustomGopSeconds: (Float) -> Unit = {},
    val onResetCustomGop: () -> Unit = {},
    val onSaveCustomAudio: (sampleRateHz: Int?, bitrateBps: Int?) -> Unit = { _, _ -> },
    val onResetCustomAudio: () -> Unit = {},
    val onPlayRecording: (RecordedVideoItem) -> Unit = {},
    val onShareRecording: (RecordedVideoItem) -> Unit = {},
    val onDeleteRecording: (RecordedVideoItem) -> Unit = {},
    val onPickCropMedia: (CropMediaCategory) -> Unit = {},
    val onBrowseCropFile: () -> Unit = {},
    val onExportCropped: (NormalizedRegion, Int, Boolean) -> Unit = { _, _, _ -> },
    val onDismissCropStatus: () -> Unit = {},
)

@Composable
fun FrameCutNavHost(
    uiState: FrameCutUiState,
    isDarkTheme: Boolean,
    callbacks: FrameCutScreenCallbacks,
    modifier: Modifier = Modifier,
    cropPreviewBitmap: ImageBitmap? = null,
    navController: NavHostController = rememberNavController(),
) {
    var showRegionOverlay by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = UiConstants.Routes.MAIN,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(UiConstants.Routes.MAIN) {
                MainScreen(
                    uiState = uiState,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = callbacks.onToggleTheme,
                    onStartRecordClick = callbacks.onStartRecordClick,
                    onPauseResumeClick = callbacks.onPauseResumeClick,
                    onStopClick = callbacks.onStopClick,
                    onOpenRecordings = {
                        navController.navigate(UiConstants.Routes.RECORDINGS) {
                            launchSingleTop = true
                        }
                    },
                    onOpenSettings = {
                        navController.navigate(UiConstants.Routes.SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                    onOpenCropTool = {
                        navController.navigate(UiConstants.Routes.CROP) {
                            launchSingleTop = true
                        }
                    },
                    onSelectMode = callbacks.onSelectMode,
                    onSelectOrientationMode = callbacks.onSelectOrientationMode,
                    onSelectAudioSource = callbacks.onSelectAudioSource,
                    onSelectBitrate = callbacks.onSelectBitrate,
                    onSelectRegionAnimation = callbacks.onSelectRegionAnimation,
                    onEditRegion = { showRegionOverlay = true },
                    onRequestOverlayPermission = callbacks.onRequestOverlayPermission,
                    onDismissMessages = callbacks.onDismissMessages,
                    onSaveCustomBitrateMbps = callbacks.onSaveCustomBitrateMbps,
                    onResetCustomBitrate = callbacks.onResetCustomBitrate,
                )
            }

            composable(UiConstants.Routes.SETTINGS) {
                SettingsScreen(
                    settings = uiState.settings,
                    isIdle = uiState.isIdle,
                    onBack = { navController.popBackStack() },
                    onSelectBitrateMode = callbacks.onSelectBitrateMode,
                    onSelectColorDepth = callbacks.onSelectColorDepth,
                    onSelectVideoFps = callbacks.onSelectVideoFps,
                    onSelectGopInterval = callbacks.onSelectGopInterval,
                    onSelectColorRange = callbacks.onSelectColorRange,
                    onSelectAudioFormat = callbacks.onSelectAudioFormat,
                    onSaveCustomFps = callbacks.onSaveCustomFps,
                    onResetCustomFps = callbacks.onResetCustomFps,
                    onSaveCustomGopSeconds = callbacks.onSaveCustomGopSeconds,
                    onResetCustomGop = callbacks.onResetCustomGop,
                    onSaveCustomAudio = callbacks.onSaveCustomAudio,
                    onResetCustomAudio = callbacks.onResetCustomAudio,
                )
            }

            composable(UiConstants.Routes.RECORDINGS) {
                RecordingsScreen(
                    recordings = uiState.recordings,
                    isLoading = uiState.isLoadingRecordings,
                    onBack = { navController.popBackStack() },
                    onPlay = callbacks.onPlayRecording,
                    onShare = callbacks.onShareRecording,
                    onDelete = callbacks.onDeleteRecording,
                )
            }

            composable(UiConstants.Routes.CROP) {
                CropToolScreen(
                    onBack = { navController.popBackStack() },
                    cropMedia = uiState.cropMedia,
                    cropPreviewBitmap = cropPreviewBitmap,
                    onPickMedia = callbacks.onPickCropMedia,
                    onBrowseAnyFile = callbacks.onBrowseCropFile,
                    onExportCropped = callbacks.onExportCropped,
                    onDismissStatus = callbacks.onDismissCropStatus,
                )
            }
        }

        if (showRegionOverlay) {
            RegionSelectorOverlay(
                initialRegion = uiState.settings.selectedRegion,
                orientationMode = uiState.settings.orientationMode,
                regionAnimation = uiState.settings.regionAnimation,
                onConfirm = { newRegion ->
                    callbacks.onUpdateSelectedRegion(newRegion)
                    showRegionOverlay = false
                },
                onCancel = { showRegionOverlay = false },
            )
        }
    }
}
