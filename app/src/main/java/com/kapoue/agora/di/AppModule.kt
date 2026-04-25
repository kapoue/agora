package com.kapoue.agora.di

import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.data.repository.QuestionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindQuestionRepository(impl: QuestionRepositoryImpl): QuestionRepository
}
