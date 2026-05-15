package com.kapoue.agora.ui.screens.game

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Question
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.domain.usecase.GetProgressUseCase
import com.kapoue.agora.domain.usecase.GetQuestionsUseCase
import com.kapoue.agora.domain.usecase.GetRandomQuestionsUseCase
import com.kapoue.agora.domain.usecase.SaveProgressUseCase
import com.kapoue.agora.ui.components.AnswerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false,
    val allDifficultiesCompleted: Boolean = false,
    val currentQuestion: Question? = null,
    val answersWithState: List<Pair<String, AnswerState>> = emptyList(),
    val selectedAnswer: String? = null,
    val showFeedback: Boolean = false,
    val showNext: Boolean = false,
    val correctAnswerText: String = "",
    val currentLevel: Int = 0,
    val theme: Theme = Theme.HISTOIRE,
    val difficulty: Difficulty = Difficulty.DEBUTANT,
    val error: String? = null,
    val totalInSession: Int = 0,
    val errorCount: Int = 0
)

@HiltViewModel
class GameViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val questionRepository: QuestionRepository,
    private val getQuestionsUseCase: GetQuestionsUseCase,
    private val getRandomQuestionsUseCase: GetRandomQuestionsUseCase,
    private val saveProgressUseCase: SaveProgressUseCase,
    private val getProgressUseCase: GetProgressUseCase,
    private val imageLoader: ImageLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    private var questions: List<Question> = emptyList()
    private var pendingQueue: ArrayDeque<Question> = ArrayDeque()
    private var lastAnswerCorrect: Boolean = false

    private var lastTheme: Theme = Theme.HISTOIRE
    private var lastDifficulty: Difficulty = Difficulty.DEBUTANT

    private var sessionErrorCount: Int = 0

    fun initialize(theme: Theme, difficulty: Difficulty) {
        lastTheme = theme
        lastDifficulty = difficulty
        sessionErrorCount = 0
        _uiState.value = GameUiState(isLoading = true, theme = theme, difficulty = difficulty)

        if (theme == Theme.CULTURE_GENERALE) {
            initializeCultureGenerale(difficulty)
            return
        }

        viewModelScope.launch {
            val progress = getProgressUseCase(theme, difficulty)
            val savedLevel = progress?.currentLevel ?: 0

            try {
                questionRepository.syncFromAssets(theme, difficulty)
            } catch (e: Exception) {
                // Continue si la DB a deja des questions
            }

            val count = questionRepository.getQuestionCount(theme, difficulty)
            if (count == 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Aucune question disponible pour ce theme et ce niveau."
                )
                return@launch
            }

            getQuestionsUseCase(theme, difficulty).first { it.isNotEmpty() }.let { loadedQuestions ->
                questions = loadedQuestions
                pendingQueue = ArrayDeque(questions.filter { !it.isAnsweredCorrectly })
                val totalInSession = pendingQueue.size

                if (pendingQueue.isEmpty()) {
                    val allDone = questionRepository.isAllDifficultiesCompleted(theme)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isCompleted = true,
                        allDifficultiesCompleted = allDone,
                        currentLevel = savedLevel,
                        totalInSession = 0
                    )
                    return@let
                }

                _uiState.value = _uiState.value.copy(totalInSession = totalInSession)
                showCurrentQuestion(savedLevel)
                prefetchImages()
            }
        }
    }

    private fun initializeCultureGenerale(difficulty: Difficulty) {
        viewModelScope.launch {
            Theme.entries
                .filter { it != Theme.CULTURE_GENERALE }
                .forEach { theme ->
                    try { questionRepository.syncFromAssets(theme, difficulty) } catch (_: Exception) {}
                }
            val loadedQuestions = getRandomQuestionsUseCase(difficulty, limit = 30)
            if (loadedQuestions.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Aucune question disponible."
                )
                return@launch
            }
            questions = loadedQuestions
            pendingQueue = ArrayDeque(loadedQuestions)
            _uiState.value = _uiState.value.copy(totalInSession = pendingQueue.size)
            showCurrentQuestion(0)
            prefetchImages()
        }
    }

    fun retry() {
        initialize(lastTheme, lastDifficulty)
    }

    private fun showCurrentQuestion(level: Int) {
        val question = pendingQueue.firstOrNull() ?: return
        val shuffled = (question.incorrectAnswers + question.correctAnswer).shuffled()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isCompleted = false,
            currentQuestion = question,
            answersWithState = shuffled.map { it to AnswerState.NORMAL },
            selectedAnswer = null,
            showFeedback = false,
            showNext = false,
            correctAnswerText = "",
            currentLevel = level
        )
    }

    fun onAnswerSelected(answer: String) {
        val state = _uiState.value
        if (state.showNext) return
        val question = state.currentQuestion ?: return
        val isCorrect = answer == question.correctAnswer
        val newLevel = if (isCorrect) state.currentLevel + 1 else state.currentLevel
        lastAnswerCorrect = isCorrect

        if (!isCorrect) {
            sessionErrorCount++
            val vibrator = context.getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        _uiState.value = state.copy(
            answersWithState = state.answersWithState.map { (ans, _) ->
                ans to when {
                    ans == question.correctAnswer -> AnswerState.CORRECT
                    ans == answer && !isCorrect -> AnswerState.WRONG_SELECTED
                    else -> AnswerState.NORMAL
                }
            },
            selectedAnswer = answer,
            showFeedback = !isCorrect,
            showNext = true,
            correctAnswerText = question.correctAnswer,
            currentLevel = newLevel
        )

        viewModelScope.launch {
            if (isCorrect && lastTheme != Theme.CULTURE_GENERALE) {
                questionRepository.markAnsweredCorrectly(question.id)
                saveProgressUseCase(
                    theme = state.theme,
                    difficulty = state.difficulty,
                    currentLevel = newLevel,
                    nextQuestionIndex = 0
                )
            }
        }
    }

    fun onNextQuestion() {
        val answered = pendingQueue.removeFirst()
        if (!lastAnswerCorrect) pendingQueue.addLast(answered)

        if (pendingQueue.isEmpty()) {
            viewModelScope.launch {
                val allDone = questionRepository.isAllDifficultiesCompleted(lastTheme)
                _uiState.value = _uiState.value.copy(
                    isCompleted = true,
                    allDifficultiesCompleted = allDone,
                    currentQuestion = null,
                    showNext = false,
                    errorCount = sessionErrorCount
                )
            }
            return
        }

        val question = pendingQueue.first()
        val shuffled = (question.incorrectAnswers + question.correctAnswer).shuffled()
        _uiState.value = _uiState.value.copy(
            isCompleted = false,
            currentQuestion = question,
            answersWithState = shuffled.map { it to AnswerState.NORMAL },
            selectedAnswer = null,
            showFeedback = false,
            showNext = false,
            correctAnswerText = ""
        )
        prefetchImages()
    }

    fun onReplay() {
        viewModelScope.launch {
            questionRepository.resetTheme(lastTheme)
            questionRepository.incrementSeriesCount(lastTheme)
        }
    }

    private fun prefetchImages() {
        listOf(1, 2, 3).forEach { offset ->
            pendingQueue.getOrNull(offset)?.imageUrl?.let { url ->
                imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
            }
        }
    }
}
