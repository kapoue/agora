package com.kapoue.agora.data.repository

import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Progress
import com.kapoue.agora.domain.model.Question
import com.kapoue.agora.domain.model.Theme
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun getQuestions(theme: Theme, difficulty: Difficulty): Flow<List<Question>>
    suspend fun syncFromAssets(theme: Theme, difficulty: Difficulty)
    suspend fun markAnsweredCorrectly(id: Long)
    suspend fun getQuestionCount(theme: Theme, difficulty: Difficulty): Int
    suspend fun getProgress(theme: Theme, difficulty: Difficulty): Progress?
    suspend fun saveProgress(progress: Progress)
    suspend fun deleteProgress(theme: Theme, difficulty: Difficulty)
    suspend fun isAllDifficultiesCompleted(theme: Theme): Boolean
    suspend fun resetTheme(theme: Theme)
    suspend fun getSeriesCount(theme: Theme): Int
    suspend fun incrementSeriesCount(theme: Theme)
    suspend fun getFirstImageUrl(theme: Theme): String?
}
