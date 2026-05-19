package com.kapoue.agora.ui.screens.multiplayer.participant

import androidx.lifecycle.ViewModel
import com.kapoue.agora.domain.model.QrPayload
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import com.kapoue.agora.ui.util.QrPayloadEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class ParticipantScanUiState(
    val sessionLoaded: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ParticipantScanViewModel @Inject constructor(
    val sessionManager: MultiplayerSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParticipantScanUiState())
    val uiState: StateFlow<ParticipantScanUiState> = _uiState

    fun onQrScanned(rawValue: String) {
        val payload: QrPayload = try {
            QrPayloadEncoder.decodePayload(rawValue)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(error = "QR code non reconnu. Réessaie.")
            return
        }

        // Charger dans le session manager
        sessionManager.reset()
        sessionManager.sessionId = payload.sessionId
        sessionManager.totalRounds = payload.totalRounds
        sessionManager.questionsPerRound = payload.questionsPerRound
        sessionManager.currentRound = payload.roundNumber
        sessionManager.setQuestionsForRound(payload.roundNumber, payload.questions)

        _uiState.value = _uiState.value.copy(sessionLoaded = true, error = null)
    }
}
