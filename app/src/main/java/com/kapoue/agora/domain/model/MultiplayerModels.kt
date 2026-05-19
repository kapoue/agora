package com.kapoue.agora.domain.model

data class WrongAnswer(
    val questionText: String,
    val correctAnswer: String,
    val givenAnswer: String
)

data class PlayerResult(
    val sessionId: String,
    val roundNumber: Int,
    val playerName: String,
    val score: Int,
    val totalQuestions: Int,
    val timeMillis: Long,
    val wrongAnswers: List<WrongAnswer>,
    val isEliminated: Boolean = false
)

data class QrPayload(
    val sessionId: String,
    val roundNumber: Int,
    val totalRounds: Int,
    val questionsPerRound: Int,
    val questions: List<MultiplayerQuestion>
)
