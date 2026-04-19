package com.kapoue.agora.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.kapoue.agora.data.local.db.entity.QuestionEntity
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Theme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private data class AssetQuestionDto(
    @SerializedName("question") val question: String,
    @SerializedName("correct_answer") val correctAnswer: String,
    @SerializedName("incorrect_answers") val incorrectAnswers: List<String>
)

@Singleton
class AssetQuestionLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    fun loadQuestions(theme: Theme, difficulty: Difficulty): List<QuestionEntity> {
        val filename = "questions/${theme.name}_${difficulty.name}.json"
        return try {
            val json = context.assets.open(filename).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<AssetQuestionDto>>() {}.type
            val dtos: List<AssetQuestionDto> = gson.fromJson(json, type)
            dtos.mapIndexed { index, dto ->
                QuestionEntity(
                    theme = theme.name,
                    difficulty = difficulty.name,
                    questionText = dto.question,
                    correctAnswer = dto.correctAnswer,
                    incorrectAnswers = gson.toJson(dto.incorrectAnswers),
                    imageUrl = null,
                    unsplashQuery = theme.unsplashQuery,
                    positionInPool = index
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractKeywords(questionText: String): String {
        val stopWords = setOf(
            "qui", "quelle", "quel", "quand", "comment", "pourquoi",
            "est", "son", "sa", "ses", "les", "des", "une", "un", "le", "la",
            "dans", "en", "de", "du", "au", "aux", "par", "sur", "avec", "pour",
            "what", "which", "when", "where", "how", "who", "the", "a", "an",
            "is", "was", "were", "are", "in", "of", "to", "and", "or"
        )
        val words = questionText.split(" ")
            .map { it.replace(Regex("[^a-zA-ZÀ-ÿ]"), "").lowercase() }
            .filter { it.length > 3 && it !in stopWords }
        return words.take(4).joinToString(" ")
    }
}
