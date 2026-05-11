package com.kapoue.agora.data.repository

import com.kapoue.agora.data.local.AssetQuestionLoader
import com.kapoue.agora.data.local.db.dao.ProgressDao
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.data.local.db.dao.ThemeProgressDao
import com.kapoue.agora.data.local.db.entity.ProgressEntity
import com.kapoue.agora.data.local.db.entity.QuestionEntity
import com.kapoue.agora.data.local.db.entity.ThemeProgressEntity
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Progress
import com.kapoue.agora.domain.model.Question
import com.kapoue.agora.domain.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepositoryImpl @Inject constructor(
    private val questionDao: QuestionDao,
    private val progressDao: ProgressDao,
    private val themeProgressDao: ThemeProgressDao,
    private val assetQuestionLoader: AssetQuestionLoader
) : QuestionRepository {

    override fun getQuestions(theme: Theme, difficulty: Difficulty): Flow<List<Question>> {
        return questionDao.getQuestions(theme.name, difficulty.name).map { entities ->
            entities.map { it.toDomain() }
        }
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
            // Nouvelles questions détectées → remise à zéro du combo pour repartir de 0
            questionDao.resetDifficultyQuestions(theme.name, difficulty.name)
            progressDao.deleteProgress(theme.name, difficulty.name)
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
        if (theme == Theme.CULTURE_GENERALE) return false
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

    override suspend fun getRandomQuestionsForAllThemes(
        difficulty: Difficulty,
        limit: Int
    ): List<Question> {
        val excluded = listOf("CULTURE_GENERALE")
        val unanswered = questionDao.getRandomUnansweredQuestionsAllThemes(
            difficulty.name, excluded, limit
        )
        return if (unanswered.size >= limit) {
            unanswered.map { it.toDomain() }
        } else {
            val remaining = limit - unanswered.size
            val answeredIds = unanswered.map { it.id }.toSet()
            val extra = questionDao.getRandomQuestionsAllThemes(
                difficulty.name, excluded, limit
            ).filter { it.id !in answeredIds }.take(remaining)
            (unanswered + extra).map { it.toDomain() }
        }
    }

    override suspend fun getRandomImageUrl(): String? {
        return questionDao.getRandomImageUrl()
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
