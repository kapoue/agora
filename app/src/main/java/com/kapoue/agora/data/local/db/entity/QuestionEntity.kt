package com.kapoue.agora.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kapoue.agora.data.local.db.Converters

@Entity(tableName = "questions")
@TypeConverters(Converters::class)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val theme: String,
    val difficulty: String,
    val questionText: String,
    val correctAnswer: String,
    val incorrectAnswers: String, // JSON array
    val imageUrl: String?,
    val unsplashQuery: String,
    val isAnsweredCorrectly: Boolean = false,
    val positionInPool: Int = 0,
    val lot: Int = 1,
    val attempts: Int = 0
)
