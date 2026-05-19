package com.kapoue.agora.domain.model

data class MultiplayerQuestion(
    val id: Long,
    val text: String,
    val correct: String,
    val wrong: List<String>,
    val category: String
)
