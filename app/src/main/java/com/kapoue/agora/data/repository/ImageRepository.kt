package com.kapoue.agora.data.repository

interface ImageRepository {
    suspend fun getImageUrl(query: String): String?
    suspend fun getImageUrls(query: String, count: Int): List<String>
}
