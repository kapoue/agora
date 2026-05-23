package com.kapoue.agora.ui.screens.multiplayer.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.kapoue.agora.BuildConfig
import com.kapoue.agora.data.local.AssetQuestionLoader
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.MultiplayerQuestion
import com.kapoue.agora.domain.model.QrPayload
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import com.kapoue.agora.ui.util.QrPayloadEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

data class ParticipantScanUiState(
    val sessionLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ParticipantScanViewModel @Inject constructor(
    val sessionManager: MultiplayerSessionManager,
    private val assetQuestionLoader: AssetQuestionLoader,
    private val questionDao: QuestionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParticipantScanUiState())
    val uiState: StateFlow<ParticipantScanUiState> = _uiState

    fun onQrScanned(rawValue: String) {
        if (_uiState.value.isLoading) return // éviter double-scan

        val payload: QrPayload = try {
            QrPayloadEncoder.decodePayload(rawValue)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(error = "QR code non reconnu. Réessaie.")
            return
        }

        // Vérification de version
        if (payload.appVersionCode != BuildConfig.VERSION_CODE) {
            val message = if (payload.appVersionCode > BuildConfig.VERSION_CODE) {
                "Mets à jour l'app pour rejoindre.\nOrganisateur : v${payload.appVersionName} — Toi : v${BuildConfig.VERSION_NAME}"
            } else {
                "L'organisateur doit mettre à jour.\nLui : v${payload.appVersionName} — Toi : v${BuildConfig.VERSION_NAME}"
            }
            _uiState.value = _uiState.value.copy(error = message)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            // Synchroniser les assets vers la DB pour les difficultés requises
            Theme.entries
                .filter { it.name !in payload.excludedThemes }
                .forEach { theme ->
                    payload.difficulties.forEach { diffName ->
                        val diff = runCatching { Difficulty.valueOf(diffName) }.getOrNull()
                            ?: return@forEach
                        try {
                            val entities = assetQuestionLoader.loadQuestions(theme, diff)
                            if (entities.isNotEmpty()) questionDao.insertQuestions(entities)
                        } catch (_: Exception) {}
                    }
                }

            // Génération déterministe depuis le seed (même algorithme que l'organisateur)
            val totalNeeded = payload.questionsPerRound * payload.totalRounds
            val allIds = questionDao.getQuestionIdsByDifficultiesAndThemes(
                difficulties = payload.difficulties,
                excludedThemes = payload.excludedThemes
            )

            if (allIds.size < totalNeeded) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Questions insuffisantes en local. Vérifie ta version de l'app."
                )
                return@launch
            }

            val allSelectedIds = allIds.sorted().shuffled(Random(payload.seed)).take(totalNeeded)
            val roundIds = allSelectedIds.chunked(payload.questionsPerRound)[payload.roundNumber - 1]

            val idToEntity = questionDao.getQuestionsByIds(roundIds).associateBy { it.id }
            val roundQuestions = roundIds.mapNotNull { id ->
                idToEntity[id]?.let { entity ->
                    MultiplayerQuestion(
                        id = entity.id,
                        text = entity.questionText.take(150),
                        correct = entity.correctAnswer.take(80),
                        wrong = Gson()
                            .fromJson(entity.incorrectAnswers, Array<String>::class.java)
                            .map { a -> a.take(80) },
                        category = entity.theme
                    )
                }
            }

            // Charger dans le session manager
            sessionManager.reset()
            sessionManager.sessionId = payload.sessionId
            sessionManager.totalRounds = payload.totalRounds
            sessionManager.questionsPerRound = payload.questionsPerRound
            sessionManager.currentRound = payload.roundNumber
            sessionManager.seed = payload.seed
            sessionManager.difficulties = payload.difficulties
            sessionManager.excludedThemes = payload.excludedThemes
            sessionManager.setQuestionsForRound(payload.roundNumber, roundQuestions)

            _uiState.value = _uiState.value.copy(isLoading = false, sessionLoaded = true)
        }
    }
}
