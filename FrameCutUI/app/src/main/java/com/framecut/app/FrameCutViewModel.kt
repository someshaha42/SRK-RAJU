package com.framecut.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.framecut.ui.core.AudioFormatOption
import com.framecut.ui.core.AudioSourceOption
import com.framecut.ui.core.BitrateModeOption
import com.framecut.ui.core.ColorDepthOption
import com.framecut.ui.core.ColorRangeOption
import com.framecut.ui.core.FrameCutUiState
import com.framecut.ui.core.GopIntervalOption
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.OrientationMode
import com.framecut.ui.core.RecordedVideoItem
import com.framecut.ui.core.RecordingMode
import com.framecut.ui.core.RecordingPhase
import com.framecut.ui.core.RecordingSettings
import com.framecut.ui.core.RegionAnimationOption
import com.framecut.ui.core.VideoBitrateOption
import com.framecut.ui.core.VideoFpsOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FrameCutViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepository = SettingsRepository(appContext)
    val recordingsRepository = RecordingsRepository(appContext)
    private val cropEngine = CropEngine(appContext)

    private val _uiState = MutableStateFlow(
        FrameCutUiState(
            settings = settingsRepository.load(),
            hasOverlayPermission = Settings.canDrawOverlays(appContext),
        ),
    )
    val uiState: StateFlow<FrameCutUiState> = _uiState.asStateFlow()

    private var selectedCropUri: Uri? = null

    init {
        viewModelScope.launch {
            RecordingController.session.collect { session ->
                _uiState.update { state -> state.copy(session = session) }
            }
        }
        viewModelScope.launch {
            RecordingController.recordingsChanged.collect {
                refreshRecordings()
            }
        }
        refreshRecordings()
    }

    fun refreshOverlayPermission() {
        val granted = Settings.canDrawOverlays(appContext)
        _uiState.update { it.copy(hasOverlayPermission = granted) }
    }

    fun refreshRecordings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecordings = true) }
            val loaded = runCatching { recordingsRepository.loadRecordings() }.getOrDefault(emptyList())
            _uiState.update { it.copy(recordings = loaded, isLoadingRecordings = false) }
        }
    }

    fun startRecordingWithProjection(resultCode: Int, data: Intent) {
        if (!_uiState.value.isIdle) return
        settingsRepository.save(_uiState.value.settings)
        val serviceIntent = ScreenRecordService.createStartIntent(appContext, resultCode, data)
        ContextCompat.startForegroundService(appContext, serviceIntent)
    }

    fun pauseOrResumeRecording() {
        when (_uiState.value.session.phase) {
            RecordingPhase.RECORDING -> ScreenRecordService.sendAction(appContext, ScreenRecordService.ACTION_PAUSE)
            RecordingPhase.PAUSED -> ScreenRecordService.sendAction(appContext, ScreenRecordService.ACTION_RESUME)
            else -> Unit
        }
    }

    fun stopRecording() {
        val phase = _uiState.value.session.phase
        if (phase == RecordingPhase.RECORDING || phase == RecordingPhase.PAUSED) {
            ScreenRecordService.sendAction(appContext, ScreenRecordService.ACTION_STOP)
        }
    }

    fun toggleTheme(systemIsDark: Boolean) {
        updateSettings { settings ->
            val currentDark = settings.darkThemeOverride ?: systemIsDark
            settings.copy(darkThemeOverride = !currentDark)
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

    fun dismissMessages() {
        RecordingController.clearMessages()
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
        viewModelScope.launch {
            val deleted = recordingsRepository.deleteRecording(item)
            if (deleted) {
                refreshRecordings()
            } else {
                notifyInfo("Unable to delete ${item.displayName}.")
            }
        }
    }

    fun notifyInfo(message: String) {
        _uiState.update { state -> state.copy(recoveryNotice = message) }
    }

    fun setSelectedCropUri(uri: Uri) {
        selectedCropUri = uri
        notifyInfo("Loaded media ready to crop and export.")
    }

    fun exportCroppedMedia(
        region: NormalizedRegion,
        qualityPercent: Int,
        preserveMetadata: Boolean,
        onCompleted: (String) -> Unit,
    ) {
        val uri = selectedCropUri
        if (uri == null) {
            onCompleted("Pick a photo or video first using Pick Media or Browse Files.")
            return
        }

        viewModelScope.launch {
            runCatching {
                cropEngine.exportMedia(uri, region, qualityPercent, preserveMetadata)
            }.onSuccess { result ->
                onCompleted(result.summary)
            }.onFailure { err ->
                onCompleted("Export failed: ${err.message ?: "Unknown error"}")
            }
        }
    }

    private inline fun updateSettings(transform: (RecordingSettings) -> RecordingSettings) {
        _uiState.update { state ->
            val updated = transform(state.settings)
            settingsRepository.save(updated)
            state.copy(settings = updated)
        }
    }
}
