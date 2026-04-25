package com.kapoue.agora.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kapoue.agora.data.local.db.dao.ProgressDao
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.data.local.db.dao.ThemeProgressDao
import com.kapoue.agora.data.local.db.entity.ProgressEntity
import com.kapoue.agora.data.local.db.entity.QuestionEntity
import com.kapoue.agora.data.local.db.entity.ThemeProgressEntity

@Database(
    entities = [QuestionEntity::class, ProgressEntity::class, ThemeProgressEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AgoraDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun progressDao(): ProgressDao
    abstract fun themeProgressDao(): ThemeProgressDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE questions ADD COLUMN lot INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE questions ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS theme_progress " +
                    "(theme TEXT NOT NULL, seriesCount INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(theme))"
                )
            }
        }
    }
}
