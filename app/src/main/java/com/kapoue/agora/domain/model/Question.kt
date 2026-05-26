package com.kapoue.agora.domain.model

data class Question(
    val id: Long = 0,
    val theme: Theme,
    val difficulty: Difficulty,
    val questionText: String,
    val correctAnswer: String,
    val incorrectAnswers: List<String>,
    val imageUrl: String? = null,
    val unsplashQuery: String,
    val isAnsweredCorrectly: Boolean = false,
    val positionInPool: Int = 0,
    val explanation: String? = null
)
