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

    @Query("UPDATE questions SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Query("SELECT COUNT(*) FROM questions WHERE theme = :theme AND difficulty = :difficulty AND isAnsweredCorrectly = 1")
    suspend fun countAnsweredCorrectly(theme: String, difficulty: String): Int

    @Query("UPDATE questions SET isAnsweredCorrectly = 0, attempts = 0 WHERE theme = :theme")
    suspend fun resetThemeQuestions(theme: String)

    @Query("SELECT imageUrl FROM questions WHERE theme = :theme AND imageUrl IS NOT NULL LIMIT 1")
    suspend fun getFirstImageUrl(theme: String): String?
}
