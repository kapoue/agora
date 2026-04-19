package com.kapoue.agora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PexelsResponseDto(
    @SerializedName("photos") val photos: List<PexelsPhotoDto>
)

data class PexelsPhotoDto(
    @SerializedName("src") val src: PexelsSrcDto
)

data class PexelsSrcDto(
    @SerializedName("large2x") val large2x: String,
    @SerializedName("original") val original: String
)
