package com.kapoue.agora.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kapoue.agora.data.local.db.dao.ProgressDao
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.data.local.db.entity.ProgressEntity
import com.kapoue.agora.data.local.db.entity.QuestionEntity

@Database(
    entities = [QuestionEntity::class, ProgressEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AgoraDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun progressDao(): ProgressDao
}
