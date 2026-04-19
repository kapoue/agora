package com.kapoue.agora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OtdResponseDto(
    @SerializedName("response_code")
    val responseCode: Int,
    @SerializedName("results")
    val results: List<OtdQuestionDto>
)

data class OtdQuestionDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("difficulty")
    val difficulty: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("question")
    val question: String,
    @SerializedName("correct_answer")
    val correctAnswer: String,
    @SerializedName("incorrect_answers")
    val incorrectAnswers: List<String>
)
