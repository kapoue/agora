package com.kapoue.agora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UnsplashResponseDto(
    @SerializedName("results")
    val results: List<UnsplashPhotoDto>
)

data class UnsplashPhotoDto(
    @SerializedName("urls")
    val urls: UnsplashUrlsDto
)

data class UnsplashUrlsDto(
    @SerializedName("regular")
    val regular: String,
    @SerializedName("small")
    val small: String
)
