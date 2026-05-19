package com.kapoue.agora.ui.screens.multiplayer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapoue.agora.domain.model.MultiplayerQuestion
import com.kapoue.agora.domain.model.WrongAnswer
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import com.kapoue.agora.ui.util.MultiplayerShuffler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MultiplayerAnswerState { NORMAL, CORRECT, WRONG_SELECTED }

data class MultiplayerGameUiState(
    val isStarted: Boolean = false,
    val isCompleted: Boolean = false,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val currentQuestion: MultiplayerQuestion? = null,
    val shuffledAnswers: List<Pair<String, MultiplayerAnswerState>> = emptyList(),
    val selectedAnswer: String? = null,
    val showFeedback: Boolean = false,
    val elapsedMillis: Long = 0L,
    val score: Int = 0,
    val wrongAnswers: List<WrongAnswer> = emptyList()
)

@HiltViewModel
class MultiplayerGameViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val sessionManager: MultiplayerSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiplayerGameUiState())
    val uiState: StateFlow<MultiplayerGameUiState> = _uiState

    private var questions: List<MultiplayerQuestion> = emptyList()
    private var startTimeMillis: Long = 0L
    private var autoAdvanceJob: Job? = null
    private var playerName: String = ""

    fun initialize(playerName: String) {
        this.playerName = playerName
        val round = sessionManager.currentRound
        questions = sessionManager.getQuestionsForRound(round)
        _uiState.value = MultiplayerGameUiState(
            isStarted = false,
            totalQuestions = questions.size,
            currentQuestionIndex = 0
        )
    }

    fun onStart() {
        startTimeMillis = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(isStarted = true)
        showQuestion(0)
    }

    private fun showQuestion(index: Int) {
        val q = questions.getOrNull(index) ?: return
        val shuffled = MultiplayerShuffler.shuffleAnswersForPlayer(
            q, playerName, sessionManager.sessionId, index
        )
        _uiState.value = _uiState.value.copy(
            currentQuestionIndex = index,
            currentQuestion = q,
            shuffledAnswers = shuffled.map { it to MultiplayerAnswerState.NORMAL },
            selectedAnswer = null,
            showFeedback = false
        )
    }

    fun onAnswerSelected(answer: String) {
        val state = _uiState.value
        if (state.selectedAnswer != null || !state.isStarted) return
        val q = state.currentQuestion ?: return
        val isCorrect = answer == q.correct

        val newScore = if (isCorrect) state.score + 1 else state.score
        val newWrongAnswers = if (!isCorrect) {
            state.wrongAnswers + WrongAnswer(
                questionText = q.text,
                correctAnswer = q.correct,
                givenAnswer = answer
            )
        } else state.wrongAnswers

        _uiState.value = state.copy(
            shuffledAnswers = state.shuffledAnswers.map { (ans, _) ->
                ans to when {
                    ans == q.correct -> MultiplayerAnswerState.CORRECT
                    ans == answer && !isCorrect -> MultiplayerAnswerState.WRONG_SELECTED
                    else -> MultiplayerAnswerState.NORMAL
                }
            },
            selectedAnswer = answer,
            showFeedback = !isCorrect,
            score = newScore,
            wrongAnswers = newWrongAnswers
        )

        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(600)
            advanceOrComplete()
        }
    }

    private fun advanceOrComplete() {
        val state = _uiState.value
        val next = state.currentQuestionIndex + 1
        if (next < questions.size) {
            showQuestion(next)
        } else {
            val elapsed = System.currentTimeMillis() - startTimeMillis
            _uiState.value = state.copy(
                isCompleted = true,
                elapsedMillis = elapsed
            )
        }
    }
}
