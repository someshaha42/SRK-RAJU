package com.framecut.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framecut.ui.definitions.UiDefinitionLoader
import com.framecut.ui.screens.navigation.FrameCutNavHost
import com.framecut.ui.screens.navigation.FrameCutScreenCallbacks
import com.framecut.ui.theme.FrameCutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: FrameCutViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = uiState.settings.darkThemeOverride ?: systemDark

            val loadedDefinitions = remember(applicationContext) {
                runCatching { UiDefinitionLoader.loadAll(applicationContext) }.getOrNull()
            }

            LaunchedEffect(loadedDefinitions) {
                // Definitions from assets/ui/*.json are loaded and ready for inspection or dynamic driving.
            }

            val callbacks = remember(viewModel, systemDark) {
                FrameCutScreenCallbacks(
                    onToggleTheme = { viewModel.toggleTheme(systemDark) },
                    onStartRecordClick = viewModel::startRecording,
                    onPauseResumeClick = viewModel::pauseOrResumeRecording,
                    onStopClick = viewModel::stopRecording,
                    onSelectMode = viewModel::selectRecordingMode,
                    onSelectOrientationMode = viewModel::selectOrientationMode,
                    onSelectAudioSource = viewModel::selectAudioSource,
                    onSelectBitrate = viewModel::selectVideoBitrate,
                    onSelectRegionAnimation = viewModel::selectRegionAnimation,
                    onUpdateSelectedRegion = viewModel::updateSelectedRegion,
                    onRequestOverlayPermission = viewModel::toggleOverlayPermission,
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
                    onPlayRecording = { item ->
                        viewModel.notifyInfo("Playing ${item.displayName}")
                    },
                    onShareRecording = { item ->
                        viewModel.notifyInfo("Sharing ${item.displayName}")
                    },
                    onDeleteRecording = viewModel::deleteRecording,
                    onPickCropMedia = viewModel::onCropMediaPicked,
                    onBrowseCropFile = {
                        viewModel.notifyInfo("Opening system document picker.")
                    },
                    onExportCropped = viewModel::onCropExportRequested,
                )
            }

            FrameCutTheme(darkTheme = isDarkTheme) {
                FrameCutNavHost(
                    uiState = uiState,
                    isDarkTheme = isDarkTheme,
                    callbacks = callbacks,
                )
            }
        }
    }
}
