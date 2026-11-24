package com.framecut.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.framecut.ui.core.RecordingPhase
import com.framecut.ui.core.RecordingSessionState
import com.framecut.ui.core.RecordingSettings
import com.framecut.ui.core.RegionAnimationOption
import com.framecut.ui.core.VideoBitrateOption
import com.framecut.ui.core.VideoFpsOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FrameCutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FrameCutUiState())
    val uiState: StateFlow<FrameCutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun toggleTheme(systemIsDark: Boolean) {
        updateSettings { settings ->
            val currentDark = settings.darkThemeOverride ?: systemIsDark
            settings.copy(darkThemeOverride = !currentDark)
        }
    }

    fun startRecording() {
        val current = _uiState.value
        if (!current.isIdle) return

        val width = if (current.settings.recordingMode == RecordingMode.FULL_SCREEN) {
            current.canvasWidthPx
        } else {
            current.selectedPixelRegion.width
        }
        val height = if (current.settings.recordingMode == RecordingMode.FULL_SCREEN) {
            current.canvasHeightPx
        } else {
            current.selectedPixelRegion.height
        }
        val audioLabel = if (current.settings.audioSource == AudioSourceOption.INTERNAL) {
            "Internal Audio"
        } else {
            "Microphone"
        }

        _uiState.update { state ->
            state.copy(
                session = RecordingSessionState(
                    phase = RecordingPhase.RECORDING,
                    elapsedMillis = 0L,
                    activeResolutionLabel = "${width}×${height}",
                    activeAudioLabel = audioLabel,
                    lastSavedName = state.session.lastSavedName,
                ),
            )
        }
        startTimerLoop()
    }

    fun pauseOrResumeRecording() {
        val phase = _uiState.value.session.phase
        when (phase) {
            RecordingPhase.RECORDING -> {
                timerJob?.cancel()
                timerJob = null
                _uiState.update { state ->
                    state.copy(session = state.session.copy(phase = RecordingPhase.PAUSED))
                }
            }
            RecordingPhase.PAUSED -> {
                _uiState.update { state ->
                    state.copy(session = state.session.copy(phase = RecordingPhase.RECORDING))
                }
                startTimerLoop()
            }
            else -> Unit
        }
    }

    fun stopRecording() {
        val snapshot = _uiState.value
        if (snapshot.session.phase != RecordingPhase.RECORDING &&
            snapshot.session.phase != RecordingPhase.PAUSED
        ) {
            return
        }

        timerJob?.cancel()
        timerJob = null

        _uiState.update { state ->
            state.copy(session = state.session.copy(phase = RecordingPhase.FINALIZING))
        }

        viewModelScope.launch {
            delay(350L)
            val nowSec = System.currentTimeMillis() / 1000L
            val fileName = "FrameCut_$nowSec.mp4"
            val width = if (snapshot.settings.recordingMode == RecordingMode.FULL_SCREEN) {
                snapshot.canvasWidthPx
            } else {
                snapshot.selectedPixelRegion.width
            }
            val height = if (snapshot.settings.recordingMode == RecordingMode.FULL_SCREEN) {
                snapshot.canvasHeightPx
            } else {
                snapshot.selectedPixelRegion.height
            }
            val durationMs = snapshot.session.elapsedMillis.coerceAtLeast(1000L)
            val estimatedBytes = ((durationMs / 1000.0) * 1_500_000.0).toLong().coerceAtLeast(250_000L)

            val newItem = RecordedVideoItem(
                id = nowSec,
                displayName = fileName,
                width = width,
                height = height,
                durationMs = durationMs,
                sizeBytes = estimatedBytes,
                dateAddedSec = nowSec,
            )

            _uiState.update { state ->
                state.copy(
                    recordings = listOf(newItem) + state.recordings,
                    session = RecordingSessionState(
                        phase = RecordingPhase.IDLE,
                        elapsedMillis = 0L,
                        lastSavedName = fileName,
                    ),
                )
            }
        }
    }

    fun selectRecordingMode(mode: RecordingMode) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(recordingMode = mode) }
    }

    fun selectOrientationMode(mode: OrientationMode) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(orientationMode = mode) }
    }

    fun selectAudioSource(source: AudioSourceOption) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(audioSource = source) }
    }

    fun selectVideoBitrate(option: VideoBitrateOption) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(videoBitrate = option, customVideoBitrateMbps = null) }
    }

    fun saveCustomVideoBitrateMbps(mbps: Long) {
        if (!_uiState.value.isIdle || mbps <= 0L) return
        updateSettings { it.copy(customVideoBitrateMbps = mbps) }
    }

    fun resetCustomVideoBitrate() {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(customVideoBitrateMbps = null) }
    }

    fun selectRegionAnimation(option: RegionAnimationOption) {
        updateSettings { it.copy(regionAnimation = option) }
    }

    fun updateSelectedRegion(region: NormalizedRegion) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(selectedRegion = region.normalized()) }
    }

    fun toggleOverlayPermission() {
        if (!_uiState.value.isIdle) return
        _uiState.update { state ->
            state.copy(hasOverlayPermission = !state.hasOverlayPermission)
        }
    }

    fun dismissMessages() {
        _uiState.update { state ->
            state.copy(
                recoveryNotice = null,
                session = state.session.copy(errorMessage = null, warningMessage = null),
            )
        }
    }

    fun selectBitrateMode(mode: BitrateModeOption) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(bitrateMode = mode) }
    }

    fun selectColorDepth(depth: ColorDepthOption) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(colorDepth = depth) }
    }

    fun selectVideoFps(fps: VideoFpsOption) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(videoFps = fps, customVideoFps = null) }
    }

    fun saveCustomVideoFps(fps: Int) {
        if (!_uiState.value.isIdle || fps <= 0) return
        updateSettings { it.copy(customVideoFps = fps) }
    }

    fun resetCustomVideoFps() {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(customVideoFps = null) }
    }

    fun selectGopInterval(gop: GopIntervalOption) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(gopInterval = gop, customGopSeconds = null) }
    }

    fun saveCustomGopSeconds(seconds: Float) {
        if (!_uiState.value.isIdle || seconds <= 0f) return
        updateSettings { it.copy(customGopSeconds = seconds) }
    }

    fun resetCustomGop() {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(customGopSeconds = null) }
    }

    fun selectColorRange(range: ColorRangeOption) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(colorRange = range) }
    }

    fun selectAudioFormat(format: AudioFormatOption) {
        if (!_uiState.value.isIdle) return
        updateSettings { it.copy(audioFormat = format) }
    }

    fun saveCustomAudio(sampleRateHz: Int?, bitrateBps: Int?) {
        if (!_uiState.value.isIdle) return
        updateSettings {
            it.copy(
                customAudioSampleRateHz = sampleRateHz,
                customAudioBitrateBps = bitrateBps,
            )
        }
    }

    fun resetCustomAudio() {
        if (!_uiState.value.isIdle) return
        updateSettings {
            it.copy(
                customAudioSampleRateHz = null,
                customAudioBitrateBps = null,
            )
        }
    }

    fun deleteRecording(item: RecordedVideoItem) {
        _uiState.update { state ->
            state.copy(recordings = state.recordings.filterNot { it.id == item.id })
        }
    }

    fun notifyInfo(message: String) {
        _uiState.update { state ->
            state.copy(recoveryNotice = message)
        }
    }

    fun onCropMediaPicked(category: CropMediaCategory) {
        notifyInfo("Selected ${category.label} media source for cropping.")
    }

    fun onCropExportRequested(region: NormalizedRegion, qualityPercent: Int, preserveMetadata: Boolean) {
        val status = if (region.isFullFrame()) "1:1 stream copy" else "cropped export"
        val metaLabel = if (preserveMetadata) "EXIF preserved" else "EXIF stripped"
        notifyInfo("Queued $status at $qualityPercent% ($metaLabel).")
    }

    private inline fun updateSettings(transform: (RecordingSettings) -> RecordingSettings) {
        _uiState.update { state -> state.copy(settings = transform(state.settings)) }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(250L)
                _uiState.update { state ->
                    if (state.session.phase == RecordingPhase.RECORDING) {
                        state.copy(
                            session = state.session.copy(
                                elapsedMillis = state.session.elapsedMillis + 250L,
                            ),
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
