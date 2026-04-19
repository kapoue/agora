package com.kapoue.agora.domain.usecase

import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Progress
import com.kapoue.agora.domain.model.Theme
import javax.inject.Inject

class SaveProgressUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(theme: Theme, difficulty: Difficulty, currentLevel: Int, nextQuestionIndex: Int) {
        questionRepository.saveProgress(
            Progress(
                theme = theme,
                difficulty = difficulty,
                currentLevel = currentLevel,
                nextQuestionIndex = nextQuestionIndex
            )
        )
    }
}
