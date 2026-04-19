package com.kapoue.agora.domain.usecase

import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Progress
import com.kapoue.agora.domain.model.Theme
import javax.inject.Inject

class GetProgressUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(theme: Theme, difficulty: Difficulty): Progress? {
        return questionRepository.getProgress(theme, difficulty)
    }
}
