package com.kapoue.agora.data.local.db.entity

import androidx.room.Entity

@Entity(
    tableName = "progress",
    primaryKeys = ["theme", "difficulty"]
)
data class ProgressEntity(
    val theme: String,
    val difficulty: String,
    val currentLevel: Int,
    val nextQuestionIndex: Int
)
