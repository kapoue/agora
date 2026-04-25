package com.kapoue.agora.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "theme_progress")
data class ThemeProgressEntity(
    @PrimaryKey
    val theme: String,
    val seriesCount: Int = 0
)
