package com.framecut.ui.core

import androidx.compose.runtime.Immutable

@Immutable
data class RecordingSessionState(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val elapsedMillis: Long = 0L,
    val activeResolutionLabel: String = "",
    val activeAudioLabel: String = "",
    val lastSavedName: String? = null,
    val errorMessage: String? = null,
    val warningMessage: String? = null,
)

@Immutable
data class RecorderDrafts(
    val bitrateDraft: String? = null,
    val bitrateBarExpanded: Boolean = false,
    val fpsDraft: String? = null,
    val fpsBarExpanded: Boolean = false,
    val gopDraft: String? = null,
    val gopBarExpanded: Boolean = false,
    val audioKhzDraft: String? = null,
    val audioKbpsDraft: String? = null,
    val audioBarExpanded: Boolean = false,
)

@Immutable
data class FrameCutUiState(
    val settings: RecordingSettings = RecordingSettings(),
    val session: RecordingSessionState = RecordingSessionState(),
    val recordings: List<RecordedVideoItem> = emptyList(),
    val isLoadingRecordings: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val recoveryNotice: String? = null,
    val drafts: RecorderDrafts = RecorderDrafts(),
) {
    val canvasWidthPx: Int
        get() = if (settings.orientationMode == OrientationMode.PORTRAIT) {
            UiConstants.DEFAULT_CANVAS_PORTRAIT_WIDTH
        } else {
            UiConstants.DEFAULT_CANVAS_LANDSCAPE_WIDTH
        }

    val canvasHeightPx: Int
        get() = if (settings.orientationMode == OrientationMode.PORTRAIT) {
            UiConstants.DEFAULT_CANVAS_PORTRAIT_HEIGHT
        } else {
            UiConstants.DEFAULT_CANVAS_LANDSCAPE_HEIGHT
        }

    val selectedPixelRegion: PixelRegion
        get() = RegionGeometry.toPixelRegion(
            region = settings.selectedRegion,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
        )

    val isIdle: Boolean
        get() = session.phase == RecordingPhase.IDLE
}
