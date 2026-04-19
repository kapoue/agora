package com.kapoue.agora.domain.model

data class Progress(
    val theme: Theme,
    val difficulty: Difficulty,
    val currentLevel: Int,
    val nextQuestionIndex: Int
)
