package com.kapoue.agora.data.remote.api

import com.kapoue.agora.data.remote.dto.PixabayResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PixabayApiService {
    @GET("api/")
    suspend fun searchPhotos(
        @Query("key") key: String,
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 10,
        @Query("image_type") imageType: String = "photo",
        @Query("orientation") orientation: String = "horizontal"
    ): PixabayResponseDto
}
