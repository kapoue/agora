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
        urlListCache[query]?.let { return it.randomOrNull() }

        return tryUnsplash(query)
            ?: tryPexels(query)
            ?: tryPixabay(query)
    }

    private suspend fun tryUnsplash(query: String): String? = try {
        val response = unsplashApiService.searchPhotos(query = query, perPage = 10)
        val urls = response.results.map { it.urls.regular }
        if (urls.isNotEmpty()) {
            urlListCache[query] = urls
            Log.d(TAG, "Unsplash OK: ${urls.size} images pour '$query'")
            urls.randomOrNull()
        } else null
    } catch (e: HttpException) {
        Log.w(TAG, "Unsplash ${e.code()} pour '$query' → fallback Pexels")
        null
    } catch (e: Exception) {
        Log.w(TAG, "Unsplash erreur pour '$query' → fallback Pexels: ${e.message}")
        null
    }

    private suspend fun tryPexels(query: String): String? = try {
        val response = pexelsApiService.searchPhotos(query = query, perPage = 10)
        val urls = response.photos.map { it.src.large2x }
        if (urls.isNotEmpty()) {
            urlListCache[query] = urls
            Log.d(TAG, "Pexels OK: ${urls.size} images pour '$query'")
            urls.randomOrNull()
        } else null
    } catch (e: HttpException) {
        Log.w(TAG, "Pexels ${e.code()} pour '$query' → fallback Pixabay")
        null
    } catch (e: Exception) {
        Log.w(TAG, "Pexels erreur pour '$query' → fallback Pixabay: ${e.message}")
        null
    }

    private suspend fun tryPixabay(query: String): String? = try {
        val response = pixabayApiService.searchPhotos(
            key = BuildConfig.PIXABAY_API_KEY,
            query = query,
            perPage = 10
        )
        val urls = response.hits.map { it.largeImageURL }
        if (urls.isNotEmpty()) {
            urlListCache[query] = urls
            Log.d(TAG, "Pixabay OK: ${urls.size} images pour '$query'")
            urls.randomOrNull()
        } else null
    } catch (e: Exception) {
        Log.w(TAG, "Pixabay erreur pour '$query': ${e.message}")
        null
    }
}
