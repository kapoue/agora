package com.kapoue.agora.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kapoue.agora.data.local.db.entity.ThemeProgressEntity

@Dao
interface ThemeProgressDao {

    @Query("SELECT seriesCount FROM theme_progress WHERE theme = :theme")
    suspend fun getSeriesCount(theme: String): Int?

    @Upsert
    suspend fun upsert(entity: ThemeProgressEntity)

    @Query("SELECT * FROM theme_progress")
    suspend fun getAll(): List<ThemeProgressEntity>
}
