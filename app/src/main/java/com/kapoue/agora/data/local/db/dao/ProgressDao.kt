package com.kapoue.agora.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kapoue.agora.data.local.db.entity.ProgressEntity

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE theme = :theme AND difficulty = :difficulty LIMIT 1")
    suspend fun getProgress(theme: String, difficulty: String): ProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE theme = :theme AND difficulty = :difficulty")
    suspend fun deleteProgress(theme: String, difficulty: String)
}
