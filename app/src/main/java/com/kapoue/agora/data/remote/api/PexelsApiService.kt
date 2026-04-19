package com.kapoue.agora.data.remote.api

import com.kapoue.agora.data.remote.dto.PexelsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PexelsApiService {
    @GET("v1/search")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 10,
        @Query("orientation") orientation: String = "landscape"
    ): PexelsResponseDto
}
