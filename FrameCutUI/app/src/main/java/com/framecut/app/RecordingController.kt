package com.framecut.app

import com.framecut.ui.core.RecordingPhase
import com.framecut.ui.core.RecordingSessionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object RecordingController {
    private val _session = MutableStateFlow(RecordingSessionState())
    val session: StateFlow<RecordingSessionState> = _session.asStateFlow()

    private val _recordingsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val recordingsChanged: SharedFlow<Unit> = _recordingsChanged.asSharedFlow()

    fun updateSession(transform: (RecordingSessionState) -> RecordingSessionState) {
        _session.update(transform)
    }

    fun setPhase(phase: RecordingPhase) {
        _session.update { it.copy(phase = phase) }
    }

    fun setElapsedMillis(elapsedMillis: Long) {
        _session.update { it.copy(elapsedMillis = elapsedMillis) }
    }

    fun setError(message: String) {
        _session.update {
            it.copy(
                phase = RecordingPhase.IDLE,
                elapsedMillis = 0L,
                errorMessage = message,
            )
        }
    }

    fun setWarning(message: String?) {
        _session.update { it.copy(warningMessage = message) }
    }

    fun clearMessages() {
        _session.update { it.copy(errorMessage = null, warningMessage = null) }
    }

    fun onRecordingSaved(displayName: String) {
        _session.update { state ->
            state.copy(
                phase = RecordingPhase.IDLE,
                elapsedMillis = 0L,
                lastSavedName = displayName,
            )
        }
        _recordingsChanged.tryEmit(Unit)
    }

    fun notifyRecordingsChanged() {
        _recordingsChanged.tryEmit(Unit)
    }
}
