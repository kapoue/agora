package com.kapoue.agora.domain.usecase

import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Question
import com.kapoue.agora.domain.model.Theme
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQuestionsUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    operator fun invoke(theme: Theme, difficulty: Difficulty): Flow<List<Question>> {
        return questionRepository.getQuestions(theme, difficulty)
    }

    suspend fun loadFromNetwork(theme: Theme, difficulty: Difficulty) {
        questionRepository.fetchAndCacheQuestions(theme, difficulty)
    }
}
