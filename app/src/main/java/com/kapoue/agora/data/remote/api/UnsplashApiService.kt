package com.kapoue.agora.data.remote.api

import com.kapoue.agora.data.remote.dto.UnsplashResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApiService {
    @GET("search/photos")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 10,
        @Query("orientation") orientation: String = "landscape"
    ): UnsplashResponseDto
}
