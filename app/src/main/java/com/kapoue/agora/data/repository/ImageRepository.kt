package com.kapoue.agora.data.repository

interface ImageRepository {
    suspend fun getImageUrl(query: String): String?
}
