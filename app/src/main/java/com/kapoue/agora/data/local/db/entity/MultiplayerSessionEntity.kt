package com.kapoue.agora.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "multiplayer_sessions")
data class MultiplayerSessionEntity(
    @PrimaryKey val sessionId: String,
    val organizerName: String,
    val totalRounds: Int,
    val questionsPerRound: Int,
    val currentRound: Int,
    val status: String,         // "IN_PROGRESS" | "COMPLETED"
    val resultsJson: String,    // JSON de List<PlayerResult> cumulés
    val eliminatedJson: String, // JSON de List<String> — noms des éliminés
    val createdAt: Long
)
