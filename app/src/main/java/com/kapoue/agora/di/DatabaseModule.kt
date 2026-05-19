package com.kapoue.agora.di

import android.content.Context
import androidx.room.Room
import com.kapoue.agora.data.local.db.AgoraDatabase
import com.kapoue.agora.data.local.db.dao.MultiplayerSessionDao
import com.kapoue.agora.data.local.db.dao.ProgressDao
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.data.local.db.dao.ThemeProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AgoraDatabase {
        return Room.databaseBuilder(
            context,
            AgoraDatabase::class.java,
            "agora_database"
        )
            .addMigrations(AgoraDatabase.MIGRATION_1_2, AgoraDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideQuestionDao(database: AgoraDatabase): QuestionDao = database.questionDao()

    @Provides
    fun provideProgressDao(database: AgoraDatabase): ProgressDao = database.progressDao()

    @Provides
    fun provideThemeProgressDao(database: AgoraDatabase): ThemeProgressDao = database.themeProgressDao()

    @Provides
    fun provideMultiplayerSessionDao(database: AgoraDatabase): MultiplayerSessionDao = database.multiplayerSessionDao()
}
