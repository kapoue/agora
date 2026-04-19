package com.kapoue.agora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PixabayResponseDto(
    @SerializedName("hits") val hits: List<PixabayHitDto>
)

data class PixabayHitDto(
    @SerializedName("largeImageURL") val largeImageURL: String,
    @SerializedName("webformatURL") val webformatURL: String
)
