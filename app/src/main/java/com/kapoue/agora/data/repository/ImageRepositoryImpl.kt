package com.kapoue.agora.data.repository

import android.util.Log
import com.kapoue.agora.BuildConfig
import com.kapoue.agora.data.remote.api.PexelsApiService
import com.kapoue.agora.data.remote.api.PixabayApiService
import com.kapoue.agora.data.remote.api.UnsplashApiService
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ImageRepo"

@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val unsplashApiService: UnsplashApiService,
    private val pexelsApiService: PexelsApiService,
    private val pixabayApiService: PixabayApiService
) : ImageRepository {

    // Cache par query → liste d'URLs (évite les appels redondants par session)
    private val urlListCache = mutableMapOf<String, List<String>>()

    override suspend fun getImageUrl(query: String): String? {
        if (urlListCache[query].isNullOrEmpty()) fetchAndCache(query, 30)
        return urlListCache[query]?.randomOrNull()
    }

    override suspend fun getImageUrls(query: String, count: Int): List<String> {
        if (urlListCache[query].isNullOrEmpty()) fetchAndCache(query, count)
        return urlListCache[query] ?: emptyList()
    }

    private suspend fun fetchAndCache(query: String, count: Int) {
        val urls = tryUnsplash(query, count)
            ?: tryPexels(query, count)
            ?: tryPixabay(query, count)
            ?: emptyList()
        if (urls.isNotEmpty()) urlListCache[query] = urls
    }

    private suspend fun tryUnsplash(query: String, perPage: Int): List<String>? = try {
        val response = unsplashApiService.searchPhotos(query = query, perPage = perPage)
        val urls = response.results.map { it.urls.regular }
        if (urls.isNotEmpty()) {
            Log.d(TAG, "Unsplash OK: ${urls.size} images pour '$query'")
            urls
        } else null
    } catch (e: HttpException) {
        Log.w(TAG, "Unsplash ${e.code()} pour '$query' → fallback Pexels")
        null
    } catch (e: Exception) {
        Log.w(TAG, "Unsplash erreur pour '$query' → fallback Pexels: ${e.message}")
        null
    }

    private suspend fun tryPexels(query: String, perPage: Int): List<String>? = try {
        val response = pexelsApiService.searchPhotos(query = query, perPage = perPage)
        val urls = response.photos.map { it.src.large2x }
        if (urls.isNotEmpty()) {
            Log.d(TAG, "Pexels OK: ${urls.size} images pour '$query'")
            urls
        } else null
    } catch (e: HttpException) {
        Log.w(TAG, "Pexels ${e.code()} pour '$query' → fallback Pixabay")
        null
    } catch (e: Exception) {
        Log.w(TAG, "Pexels erreur pour '$query' → fallback Pixabay: ${e.message}")
        null
    }

    private suspend fun tryPixabay(query: String, perPage: Int): List<String>? = try {
        val response = pixabayApiService.searchPhotos(
            key = BuildConfig.PIXABAY_API_KEY,
            query = query,
            perPage = perPage
        )
        val urls = response.hits.map { it.largeImageURL }
        if (urls.isNotEmpty()) {
            Log.d(TAG, "Pixabay OK: ${urls.size} images pour '$query'")
            urls
        } else null
    } catch (e: Exception) {
        Log.w(TAG, "Pixabay erreur pour '$query': ${e.message}")
        null
    }
}
