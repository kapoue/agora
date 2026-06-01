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
import com.kapoue.agora.ui.util.AppLogger
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
    val hasMoreQuestions: Boolean = false,
    val currentQuestion: Question? = null,
    val answersWithState: List<Pair<String, AnswerState>> = emptyList(),
    val selectedAnswer: String? = null,
    val showFeedback: Boolean = false,
    val showNext: Boolean = false,
    val correctAnswerText: String = "",
    val explanation: String? = null,
    val currentLevel: Int = 0,
    val theme: Theme = Theme.HISTOIRE,
    val difficulty: Difficulty = Difficulty.DEBUTANT,
    val error: String? = null,
    val isComingSoon: Boolean = false,
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
    private val imageLoader: ImageLoader,
    private val logger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    private var questions: List<Question> = emptyList()
    private var pendingQueue: ArrayDeque<Question> = ArrayDeque()
    private var lastAnswerCorrect: Boolean = false

    private var lastTheme: Theme = Theme.HISTOIRE
    private var lastDifficulty: Difficulty = Difficulty.DEBUTANT

    private var sessionErrorCount: Int = 0
    private var sessionHasMore: Boolean = false

    companion object {
        private const val SESSION_SIZE = 20
        private const val TAG = "GameViewModel"
    }

    fun initialize(theme: Theme, difficulty: Difficulty) {
        lastTheme = theme
        lastDifficulty = difficulty
        sessionErrorCount = 0
        _uiState.value = GameUiState(isLoading = true, theme = theme, difficulty = difficulty)
        logger.i(TAG, "initialize — thème=${theme.name} difficulté=${difficulty.name}")

        if (theme == Theme.CULTURE_GENERALE) {
            initializeCultureGenerale(difficulty)
            return
        }

        viewModelScope.launch {
            val progress = getProgressUseCase(theme, difficulty)
            val savedLevel = progress?.currentLevel ?: 0
            logger.d(TAG, "progression chargée — niveau=$savedLevel")

            var noJsonInAssets = false
            try {
                questionRepository.syncFromAssets(theme, difficulty)
            } catch (e: Exception) {
                if (e.message?.contains("Aucune question trouvee") == true) {
                    noJsonInAssets = true
                    logger.w(TAG, "syncFromAssets — pas de JSON assets pour ${theme.name}/${difficulty.name}")
                } else {
                    logger.w(TAG, "syncFromAssets ignoré (DB déjà à jour) — ${e.message}")
                }
            }

            val count = questionRepository.getQuestionCount(theme, difficulty)
            logger.i(TAG, "questions en DB — total=$count")

            if (count == 0) {
                logger.e(TAG, "aucune question disponible pour ${theme.name}/${difficulty.name}")
                val errorMsg = if (noJsonInAssets)
                    "Les questions de ce thème arrivent bientôt !\nEn attendant, explore les autres thèmes."
                else
                    "Aucune question disponible pour ce thème et ce niveau."
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg,
                    isComingSoon = noJsonInAssets
                )
                return@launch
            }

            getQuestionsUseCase(theme, difficulty).first { it.isNotEmpty() }.let { loadedQuestions ->
                questions = loadedQuestions
                val allUnanswered = questions.filter { !it.isAnsweredCorrectly }
                sessionHasMore = allUnanswered.size > SESSION_SIZE
                pendingQueue = ArrayDeque(allUnanswered.take(SESSION_SIZE))
                val totalInSession = pendingQueue.size
                logger.i(TAG, "questions chargées — total=${questions.size} nonRépondues=${allUnanswered.size} session=$totalInSession hasMore=$sessionHasMore")

                if (pendingQueue.isEmpty()) {
                    logger.i(TAG, "toutes les questions ont été répondues — session terminée")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isCompleted = true,
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
            logger.i(TAG, "initializeCultureGenerale — difficulté=${difficulty.name}")
            Theme.entries
                .filter { it != Theme.CULTURE_GENERALE }
                .forEach { theme ->
                    val count = questionRepository.getQuestionCount(theme, difficulty)
                    if (count == 0) {
                        try {
                            questionRepository.syncFromAssets(theme, difficulty)
                            logger.d(TAG, "sync CG — ${theme.name}/${difficulty.name} inséré depuis assets")
                        } catch (_: Exception) {
                            logger.w(TAG, "sync CG — ${theme.name}/${difficulty.name} ignoré")
                        }
                    } else {
                        logger.d(TAG, "sync CG — ${theme.name}/${difficulty.name} déjà en DB ($count questions), sync ignoré")
                    }
                }

            val loadedQuestions = getRandomQuestionsUseCase(difficulty, limit = SESSION_SIZE)
            logger.i(TAG, "CG — questions piochées=${loadedQuestions.size} (limit=$SESSION_SIZE)")

            if (loadedQuestions.isEmpty()) {
                logger.e(TAG, "CG — aucune question disponible")
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

    fun onNextSession() {
        logger.i(TAG, "onNextSession — thème=${lastTheme.name} difficulté=${lastDifficulty.name}")
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

        logger.d(TAG, "réponse — question=\"${question.questionText.take(50)}\" correct=$isCorrect niveau=$newLevel")

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
            explanation = question.explanation,
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
        if (!_uiState.value.showNext) return
        val answered = pendingQueue.removeFirst()
        if (!lastAnswerCorrect) pendingQueue.addLast(answered)

        if (pendingQueue.isEmpty()) {
            logger.i(TAG, "session terminée — erreurs=$sessionErrorCount")
            _uiState.value = _uiState.value.copy(
                isCompleted = true,
                hasMoreQuestions = sessionHasMore,
                currentQuestion = null,
                showNext = false,
                errorCount = sessionErrorCount
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
                imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
            }
        }
    }
}
