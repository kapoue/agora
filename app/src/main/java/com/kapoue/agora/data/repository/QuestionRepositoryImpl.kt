package com.kapoue.agora.data.repository

import android.os.Build
import android.text.Html
import com.kapoue.agora.data.local.AssetQuestionLoader
import com.kapoue.agora.data.local.db.dao.ProgressDao
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.data.local.db.entity.ProgressEntity
import com.kapoue.agora.data.local.db.entity.QuestionEntity
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
            // Délai progressif : 0s, 6s, 12s
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
                            lastError = Exception("L'API est surchargée (rate limit). Patiente quelques secondes.")
                            return@repeat // retry l'attempt suivant
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
                            return // succès
                        }
                        amount == 50 -> continue // essaie avec 20
                        else -> {
                            lastError = Exception("Aucune question disponible pour ${theme.displayName} / ${difficulty.displayName}.")
                            return@repeat
                        }
                    }
                } catch (e: HttpException) {
                    if (e.code() == 429) {
                        lastError = Exception("L'API est surchargée (429). Patiente quelques secondes.")
                        return@repeat // retry
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
            throw Exception("Aucune question trouvée dans les assets pour ${theme.displayName} / ${difficulty.displayName}.")
        }
        val existingTexts = questionDao.getQuestionTexts(theme.name, difficulty.name).toSet()
        val existingCount = existingTexts.size
        val toInsert = newEntities
            .filter { it.questionText !in existingTexts }
            .mapIndexed { index, entity -> entity.copy(positionInPool = existingCount + index) }
        if (toInsert.isNotEmpty()) {
            questionDao.insertQuestions(toInsert)
        }
        // Normalise les questions existantes sans image vers la query du thème
        questionDao.normalizeUnsplashQuery(theme.name, theme.unsplashQuery)
    }

    override suspend fun markAnsweredCorrectly(id: Long) {
        questionDao.markAnsweredCorrectly(id)
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

    override suspend fun updateQuestionImageUrl(questionId: Long, imageUrl: String) {
        val questions = questionDao.getQuestionsList("", "")
        // Update is done via the entity update
        val allEntities = mutableListOf<QuestionEntity>()
        // We need a targeted update - handled in GameViewModel
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
            "qui", "quelle", "quel", "quand", "où", "comment", "pourquoi",
            "est", "son", "sa", "ses", "les", "des", "une", "un", "le", "la",
            "dans", "en", "de", "du", "au", "aux", "par", "sur", "avec", "pour",
            "what", "which", "when", "where", "how", "who", "the", "a", "an",
            "is", "was", "were", "are", "in", "of", "to", "and", "or"
        )
        val words = questionText.split(" ")
            .map { it.replace(Regex("[^a-zA-ZÀ-ÿ]"), "").lowercase() }
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
