package com.kapoue.agora.ui.screens.multiplayer.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapoue.agora.data.local.AssetQuestionLoader
import com.kapoue.agora.data.local.datastore.UserPreferencesDataStore
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.MultiplayerQuestion
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrganizerSetupUiState(
    val name: String = "",
    val questionsPerRound: Int = 10,
    val totalRounds: Int = 1,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val isReady: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OrganizerSetupViewModel @Inject constructor(
    private val sessionManager: MultiplayerSessionManager,
    private val questionDao: QuestionDao,
    private val assetQuestionLoader: AssetQuestionLoader,
    private val dataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizerSetupUiState())
    val uiState: StateFlow<OrganizerSetupUiState> = _uiState

    companion object {
        val KEY_PLAYER_NAME = stringPreferencesKey("player_name")
        private val EXCLUDED_THEMES = listOf(Theme.CULTURE_GENERALE.name)
        private val DIFFICULTIES = listOf(Difficulty.DEBUTANT.name, Difficulty.MOYEN.name)
    }

    init {
        viewModelScope.launch {
            val prefs = dataStore.dataStore.data.first()
            val savedName = prefs[KEY_PLAYER_NAME] ?: ""
            _uiState.value = _uiState.value.copy(name = savedName)
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onQuestionsChange(count: Int) {
        _uiState.value = _uiState.value.copy(questionsPerRound = count)
    }

    fun onRoundsChange(count: Int) {
        _uiState.value = _uiState.value.copy(totalRounds = count)
    }

    fun onConfirm() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Saisis ton prénom pour continuer.")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            // Sauvegarder le prénom
            dataStore.dataStore.edit { it[KEY_PLAYER_NAME] = state.name.trim() }

            // Initialiser le session manager
            sessionManager.reset()
            sessionManager.organizerName = state.name.trim()
            sessionManager.totalRounds = state.totalRounds
            sessionManager.questionsPerRound = state.questionsPerRound

            // Synchroniser les assets vers la DB (no-op si déjà fait)
            _uiState.value = _uiState.value.copy(loadingMessage = "Chargement des questions…")
            Theme.entries
                .filter { it != Theme.CULTURE_GENERALE }
                .forEach { theme ->
                    listOf(Difficulty.DEBUTANT, Difficulty.MOYEN).forEach { diff ->
                        try {
                            val entities = assetQuestionLoader.loadQuestions(theme, diff)
                            if (entities.isNotEmpty()) questionDao.insertQuestions(entities)
                        } catch (_: Exception) {}
                    }
                }

            // Charger les questions pour toutes les manches (Débutant + Moyen mélangés)
            val totalNeeded = state.questionsPerRound * state.totalRounds
            val entities = questionDao.getRandomQuestionsMultipleDifficulties(
                difficulties = DIFFICULTIES,
                excludedThemes = EXCLUDED_THEMES,
                limit = totalNeeded
            )

            if (entities.size < totalNeeded) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = "",
                    error = "Pas assez de questions disponibles dans les assets."
                )
                return@launch
            }

            val allQuestions = entities.map {
                MultiplayerQuestion(
                    id = it.id,
                    text = it.questionText.take(150),
                    correct = it.correctAnswer.take(80),
                    wrong = Gson()
                        .fromJson(it.incorrectAnswers, Array<String>::class.java)
                        .map { a -> a.take(80) },
                    category = it.theme
                )
            }

            // Distribuer les questions par manche
            allQuestions.chunked(state.questionsPerRound).forEachIndexed { index, roundQs ->
                sessionManager.setQuestionsForRound(index + 1, roundQs)
            }

            sessionManager.persistSession()

            _uiState.value = _uiState.value.copy(isLoading = false, loadingMessage = "", isReady = true)
        }
    }
}
