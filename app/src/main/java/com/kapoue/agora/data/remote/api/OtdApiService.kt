package com.kapoue.agora.data.remote.api

import com.kapoue.agora.data.remote.dto.OtdResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OtdApiService {
    @GET("api.php")
    suspend fun getQuestions(
        @Query("amount") amount: Int = 50,
        @Query("category") category: Int,
        @Query("difficulty") difficulty: String,
        @Query("type") type: String = "multiple"
    ): OtdResponseDto
}
