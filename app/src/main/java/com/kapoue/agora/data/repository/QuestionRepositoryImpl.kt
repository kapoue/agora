package com.kapoue.agora.data.repository

import android.os.Build
import android.text.Html
import com.kapoue.agora.data.local.AssetQuestionLoader
import com.kapoue.agora.data.local.db.dao.ProgressDao
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.data.local.db.dao.ThemeProgressDao
import com.kapoue.agora.data.local.db.entity.ProgressEntity
import com.kapoue.agora.data.local.db.entity.QuestionEntity
import com.kapoue.agora.data.local.db.entity.ThemeProgressEntity
import com.kapoue.agora.data.remote.api.OtdApiService
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Progress
import com.kapoue.agora.domain.model.Question
import com.kapoue.agora.domain.model.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepositoryImpl @Inject constructor(
    private val questionDao: QuestionDao,
    private val progressDao: ProgressDao,
    private val themeProgressDao: ThemeProgressDao,
    private val otdApiService: OtdApiService,
    private val assetQuestionLoader: AssetQuestionLoader
) : QuestionRepository {

    override fun getQuestions(theme: Theme, difficulty: Difficulty): Flow<List<Question>> {
        return questionDao.getQuestions(theme.name, difficulty.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchAndCacheQuestions(theme: Theme, difficulty: Difficulty) {
        val amounts = listOf(50, 20)
        var lastError: Exception = Exception("Erreur inconnue")

        repeat(3) { attempt ->
            if (attempt > 0) delay(6000L * attempt)

            for (amount in amounts) {
                try {
                    val response = otdApiService.getQuestions(
                        amount = amount,
                        category = theme.otdCategoryId,
                        difficulty = difficulty.otdValue
                    )
                    when {
                        response.responseCode == 5 -> {
                            lastError = Exception("L''API est surchargee (rate limit).")
                            return@repeat
                        }
                        response.responseCode == 0 && response.results.isNotEmpty() -> {
                            val entities = response.results.mapIndexed { index, dto ->
                                QuestionEntity(
                                    theme = theme.name,
                                    difficulty = difficulty.name,
                                    questionText = decodeHtml(dto.question),
                                    correctAnswer = decodeHtml(dto.correctAnswer),
                                    incorrectAnswers = com.google.gson.Gson().toJson(
                                        dto.incorrectAnswers.map { decodeHtml(it) }
                                    ),
                                    imageUrl = null,
                                    unsplashQuery = extractKeywords(decodeHtml(dto.question)),
                                    positionInPool = index
                                )
                            }
                            questionDao.insertQuestions(entities)
                            return
                        }
                        amount == 50 -> continue
                        else -> {
                            lastError = Exception("Aucune question disponible pour ${theme.displayName} / ${difficulty.displayName}.")
                            return@repeat
                        }
                    }
                } catch (e: HttpException) {
                    if (e.code() == 429) {
                        lastError = Exception("L''API est surchargee (429).")
                        return@repeat
                    }
                    throw e
                }
            }
        }
        throw lastError
    }

    override suspend fun syncFromAssets(theme: Theme, difficulty: Difficulty) {
        val newEntities = assetQuestionLoader.loadQuestions(theme, difficulty)
        if (newEntities.isEmpty()) {
            throw Exception("Aucune question trouvee dans les assets pour ${theme.displayName} / ${difficulty.displayName}.")
        }
        val existingTexts = questionDao.getQuestionTexts(theme.name, difficulty.name).toSet()
        val existingCount = existingTexts.size

        val toInsert = newEntities
            .filter { it.questionText !in existingTexts }
            .mapIndexed { index, entity -> entity.copy(positionInPool = existingCount + index) }
        if (toInsert.isNotEmpty()) {
            questionDao.insertQuestions(toInsert)
        }

        newEntities
            .filter { it.questionText in existingTexts && it.imageUrl != null }
            .forEach { entity ->
                questionDao.updateImageUrl(theme.name, difficulty.name, entity.questionText, entity.imageUrl)
            }
    }

    override suspend fun markAnsweredCorrectly(id: Long) {
        questionDao.markAnsweredCorrectly(id)
    }

    override suspend fun incrementAttempts(id: Long) {
        questionDao.incrementAttempts(id)
    }

    override suspend fun getQuestionCount(theme: Theme, difficulty: Difficulty): Int {
        return questionDao.countQuestions(theme.name, difficulty.name)
    }

    override suspend fun getProgress(theme: Theme, difficulty: Difficulty): Progress? {
        return progressDao.getProgress(theme.name, difficulty.name)?.let {
            Progress(
                theme = theme,
                difficulty = difficulty,
                currentLevel = it.currentLevel,
                nextQuestionIndex = it.nextQuestionIndex
            )
        }
    }

    override suspend fun saveProgress(progress: Progress) {
        progressDao.upsertProgress(
            ProgressEntity(
                theme = progress.theme.name,
                difficulty = progress.difficulty.name,
                currentLevel = progress.currentLevel,
                nextQuestionIndex = progress.nextQuestionIndex
            )
        )
    }

    override suspend fun deleteProgress(theme: Theme, difficulty: Difficulty) {
        progressDao.deleteProgress(theme.name, difficulty.name)
    }

    override suspend fun isAllDifficultiesCompleted(theme: Theme): Boolean {
        return Difficulty.entries.all { difficulty ->
            val total = questionDao.countQuestions(theme.name, difficulty.name)
            val answered = questionDao.countAnsweredCorrectly(theme.name, difficulty.name)
            total > 0 && answered >= total
        }
    }

    override suspend fun resetTheme(theme: Theme) {
        questionDao.resetThemeQuestions(theme.name)
        Difficulty.entries.forEach { difficulty ->
            progressDao.deleteProgress(theme.name, difficulty.name)
        }
    }

    override suspend fun getSeriesCount(theme: Theme): Int {
        return themeProgressDao.getSeriesCount(theme.name) ?: 0
    }

    override suspend fun incrementSeriesCount(theme: Theme) {
        val current = themeProgressDao.getSeriesCount(theme.name) ?: 0
        themeProgressDao.upsert(ThemeProgressEntity(theme = theme.name, seriesCount = current + 1))
    }

    override suspend fun getFirstImageUrl(theme: Theme): String? {
        return questionDao.getFirstImageUrl(theme.name)
    }

    private fun decodeHtml(text: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(text).toString()
        }
    }

    private fun extractKeywords(questionText: String): String {
        val stopWords = setOf(
            "qui", "quelle", "quel", "quand", "ou", "comment", "pourquoi",
            "est", "son", "sa", "ses", "les", "des", "une", "un", "le", "la",
            "dans", "en", "de", "du", "au", "aux", "par", "sur", "avec", "pour",
            "what", "which", "when", "where", "how", "who", "the", "a", "an",
            "is", "was", "were", "are", "in", "of", "to", "and", "or"
        )
        val words = questionText.split(" ")
            .map { it.replace(Regex("[^a-zA-ZA-z]"), "").lowercase() }
            .filter { it.length > 3 && it !in stopWords }
        return words.take(4).joinToString(" ")
    }
}

private fun QuestionEntity.toDomain(): Question {
    val gson = com.google.gson.Gson()
    val incorrectList: List<String> = try {
        gson.fromJson(incorrectAnswers, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
    } catch (e: Exception) {
        emptyList()
    }
    return Question(
        id = id,
        theme = Theme.valueOf(theme),
        difficulty = Difficulty.valueOf(difficulty),
        questionText = questionText,
        correctAnswer = correctAnswer,
        incorrectAnswers = incorrectList,
        imageUrl = imageUrl,
        unsplashQuery = unsplashQuery,
        isAnsweredCorrectly = isAnsweredCorrectly,
        positionInPool = positionInPool
    )
}
