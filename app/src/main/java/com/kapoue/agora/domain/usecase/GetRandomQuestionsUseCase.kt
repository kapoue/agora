package com.kapoue.agora.domain.usecase

import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Question
import javax.inject.Inject

class GetRandomQuestionsUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(difficulty: Difficulty, limit: Int = 20): List<Question> {
        return questionRepository.getRandomQuestionsForAllThemes(difficulty, limit)
    }
}
