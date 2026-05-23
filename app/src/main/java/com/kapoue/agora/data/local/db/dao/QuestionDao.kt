package com.kapoue.agora.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kapoue.agora.data.local.db.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE theme = :theme AND difficulty = :difficulty ORDER BY positionInPool ASC")
    fun getQuestions(theme: String, difficulty: String): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Query("SELECT COUNT(*) FROM questions WHERE theme = :theme AND difficulty = :difficulty")
    suspend fun countQuestions(theme: String, difficulty: String): Int

    @Query("SELECT * FROM questions WHERE theme = :theme AND difficulty = :difficulty ORDER BY positionInPool ASC")
    suspend fun getQuestionsList(theme: String, difficulty: String): List<QuestionEntity>

    @Query("UPDATE questions SET isAnsweredCorrectly = 1 WHERE id = :id")
    suspend fun markAnsweredCorrectly(id: Long)

    @Query("SELECT questionText FROM questions WHERE theme = :theme AND difficulty = :difficulty")
    suspend fun getQuestionTexts(theme: String, difficulty: String): List<String>

    @Query("UPDATE questions SET unsplashQuery = :query WHERE theme = :theme AND imageUrl IS NULL")
    suspend fun normalizeUnsplashQuery(theme: String, query: String)

    @Query("UPDATE questions SET imageUrl = :imageUrl WHERE theme = :theme AND difficulty = :difficulty AND questionText = :questionText")
    suspend fun updateImageUrl(theme: String, difficulty: String, questionText: String, imageUrl: String?)

    @Query("SELECT COUNT(*) FROM questions WHERE theme = :theme AND difficulty = :difficulty AND isAnsweredCorrectly = 1")
    suspend fun countAnsweredCorrectly(theme: String, difficulty: String): Int

    @Query("UPDATE questions SET isAnsweredCorrectly = 0 WHERE theme = :theme AND difficulty = :difficulty")
    suspend fun resetDifficultyQuestions(theme: String, difficulty: String)

    @Query("UPDATE questions SET isAnsweredCorrectly = 0, attempts = 0 WHERE theme = :theme")
    suspend fun resetThemeQuestions(theme: String)

    @Query("SELECT imageUrl FROM questions WHERE theme = :theme AND imageUrl IS NOT NULL LIMIT 1")
    suspend fun getFirstImageUrl(theme: String): String?

    @Query("""
        SELECT * FROM questions
        WHERE difficulty = :difficulty AND theme NOT IN (:excludedThemes)
        AND isAnsweredCorrectly = 0
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomUnansweredQuestionsAllThemes(
        difficulty: String,
        excludedThemes: List<String>,
        limit: Int
    ): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions
        WHERE difficulty = :difficulty AND theme NOT IN (:excludedThemes)
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomQuestionsAllThemes(
        difficulty: String,
        excludedThemes: List<String>,
        limit: Int
    ): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions
        WHERE difficulty IN (:difficulties) AND theme NOT IN (:excludedThemes)
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomQuestionsMultipleDifficulties(
        difficulties: List<String>,
        excludedThemes: List<String>,
        limit: Int
    ): List<QuestionEntity>

    @Query("SELECT imageUrl FROM questions WHERE theme != 'CULTURE_GENERALE' AND imageUrl IS NOT NULL ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomImageUrl(): String?

    @Query("""
        SELECT id FROM questions
        WHERE difficulty IN (:difficulties) AND theme NOT IN (:excludedThemes)
        ORDER BY id ASC
    """)
    suspend fun getQuestionIdsByDifficultiesAndThemes(
        difficulties: List<String>,
        excludedThemes: List<String>
    ): List<Long>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getQuestionsByIds(ids: List<Long>): List<QuestionEntity>
}
