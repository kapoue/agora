package com.kapoue.agora.ui.screens.multiplayer.organizer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapoue.agora.domain.model.PlayerResult
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import com.kapoue.agora.ui.util.QrPayloadEncoder
import com.kapoue.agora.ui.util.VibrationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannedPlayer(
    val name: String,
    val score: Int,
    val totalQuestions: Int,
    val timeMillis: Long
)

data class OrganizerScanUiState(
    val scannedPlayers: List<ScannedPlayer> = emptyList(),
    val duplicateError: String? = null,
    val invalidQrError: String? = null
)

@HiltViewModel
class OrganizerScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val sessionManager: MultiplayerSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizerScanUiState())
    val uiState: StateFlow<OrganizerScanUiState> = _uiState

    fun onQrScanned(rawValue: String) {
        val result = try {
            QrPayloadEncoder.decodeResult(rawValue)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(invalidQrError = "QR code non reconnu")
            return
        }

        // Sécurité : vérifier sessionId et roundNumber
        if (result.sessionId != sessionManager.sessionId) {
            _uiState.value = _uiState.value.copy(invalidQrError = "Ce QR code appartient à une autre session")
            return
        }
        if (result.roundNumber != sessionManager.currentRound) {
            _uiState.value = _uiState.value.copy(invalidQrError = "Ce QR code est pour la manche ${result.roundNumber}, pas ${sessionManager.currentRound}")
            return
        }

        val currentScanned = _uiState.value.scannedPlayers
        if (currentScanned.any { it.name == result.playerName }) {
            _uiState.value = _uiState.value.copy(
                duplicateError = "${result.playerName} a déjà été scanné"
            )
            return
        }

        VibrationHelper.vibrateOnScan(context)

        val player = ScannedPlayer(
            name = result.playerName,
            score = result.score,
            totalQuestions = result.totalQuestions,
            timeMillis = result.timeMillis
        )

        _uiState.value = _uiState.value.copy(
            scannedPlayers = currentScanned + player,
            duplicateError = null,
            invalidQrError = null
        )

        // Enregistrer dans le session manager
        sessionManager.addRoundResult(
            playerName = result.playerName,
            score = result.score,
            totalQuestions = result.totalQuestions,
            timeMillis = result.timeMillis,
            wrongAnswers = result.wrongAnswers
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(duplicateError = null, invalidQrError = null)
    }

    fun onDone() {
        viewModelScope.launch {
            sessionManager.eliminateAbsentPlayers()
            sessionManager.commitRoundResults()
            sessionManager.updatePersistedSession()
        }
    }
}
