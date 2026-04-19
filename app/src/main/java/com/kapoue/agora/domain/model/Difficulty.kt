package com.kapoue.agora.domain.model

enum class Difficulty(
    val displayName: String,
    val otdValue: String
) {
    DEBUTANT(displayName = "Débutant", otdValue = "easy"),
    MOYEN(displayName = "Moyen", otdValue = "medium"),
    EXPERT(displayName = "Expert", otdValue = "hard")
}
