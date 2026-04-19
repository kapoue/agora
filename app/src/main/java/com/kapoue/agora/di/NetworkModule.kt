package com.kapoue.agora.di

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.kapoue.agora.BuildConfig
import com.kapoue.agora.data.remote.api.OtdApiService
import com.kapoue.agora.data.remote.api.PexelsApiService
import com.kapoue.agora.data.remote.api.PixabayApiService
import com.kapoue.agora.data.remote.api.UnsplashApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    @Named("otd")
    fun provideOtdOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("unsplash")
    fun provideUnsplashOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val key = BuildConfig.UNSPLASH_API_KEY
            Log.d("UnsplashNet", "Unsplash key length=${key.length}, isEmpty=${key.isEmpty()}")
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Client-ID $key")
                .build()
            val response = chain.proceed(request)
            Log.d("UnsplashNet", "Response code=${response.code} url=${request.url}")
            response
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideOtdApiService(@Named("otd") okHttpClient: OkHttpClient): OtdApiService {
        return Retrofit.Builder()
            .baseUrl("https://opentdb.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OtdApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUnsplashApiService(@Named("unsplash") okHttpClient: OkHttpClient): UnsplashApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.unsplash.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UnsplashApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("pexels")
    fun providePexelsOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", BuildConfig.PEXELS_API_KEY)
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun providePexelsApiService(@Named("pexels") okHttpClient: OkHttpClient): PexelsApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PexelsApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePixabayApiService(): PixabayApiService {
        return Retrofit.Builder()
            .baseUrl("https://pixabay.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PixabayApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return SingletonImageLoader.get(context)
    }
}
