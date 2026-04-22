package com.kapoue.agora.ui.screens.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import com.kapoue.agora.data.repository.ImageRepository
import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.data.repository.SharedImagePool
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Question
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.domain.usecase.GetProgressUseCase
import com.kapoue.agora.domain.usecase.GetQuestionsUseCase
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
    val currentQuestion: Question? = null,
    val answersWithState: List<Pair<String, AnswerState>> = emptyList(),
    val selectedAnswer: String? = null,
    val showFeedback: Boolean = false,
    val showNext: Boolean = false,
    val correctAnswerText: String = "",
    val currentLevel: Int = 0,
    val theme: Theme = Theme.HISTOIRE,
    val difficulty: Difficulty = Difficulty.DEBUTANT,
    val error: String? = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val questionRepository: QuestionRepository,
    private val imageRepository: ImageRepository,
    private val sharedImagePool: SharedImagePool,
    private val getQuestionsUseCase: GetQuestionsUseCase,
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

    fun initialize(theme: Theme, difficulty: Difficulty) {
        lastTheme = theme
        lastDifficulty = difficulty
        _uiState.value = GameUiState(
            isLoading = true,
            theme = theme,
            difficulty = difficulty
        )
        viewModelScope.launch {
            val progress = getProgressUseCase(theme, difficulty)
            val savedLevel = progress?.currentLevel ?: 0

            // Sync différentielle : insère uniquement les nouvelles questions depuis les assets
            try {
                questionRepository.syncFromAssets(theme, difficulty)
            } catch (e: Exception) {
                // On continue — si la DB a déjà des questions, on les utilise
            }

            val count = questionRepository.getQuestionCount(theme, difficulty)
            if (count == 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Aucune question disponible pour ce thème et ce niveau."
                )
                return@launch
            }

            getQuestionsUseCase(theme, difficulty).first { it.isNotEmpty() }.let { loadedQuestions ->
                // Assigner les URLs du pool par positionInPool (séquentiel, sans répétition)
                questions = loadedQuestions.map { q ->
                    q.copy(imageUrl = sharedImagePool.getUrl(q.positionInPool) ?: q.imageUrl)
                }
                pendingQueue = ArrayDeque(
                    questions.filter { !it.isAnsweredCorrectly }
                )

                if (pendingQueue.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isCompleted = true,
                        currentLevel = savedLevel
                    )
                    return@let
                }

                showCurrentQuestion(savedLevel)
                prefetchImages()
            }
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

        if (isCorrect) {
            viewModelScope.launch {
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
        if (!lastAnswerCorrect) {
            // Mauvaise réponse : remettre en fin de queue
            pendingQueue.addLast(answered)
        }

        if (pendingQueue.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isCompleted = true,
                currentQuestion = null,
                showNext = false
            )
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

    private fun prefetchImages() {
        listOf(1, 2, 3).forEach { offset ->
            pendingQueue.getOrNull(offset)?.imageUrl?.let { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }
}
