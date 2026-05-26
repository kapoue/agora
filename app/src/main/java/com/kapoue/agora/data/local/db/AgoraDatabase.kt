package com.kapoue.agora.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kapoue.agora.data.local.db.dao.MultiplayerSessionDao
import com.kapoue.agora.data.local.db.dao.ProgressDao
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.data.local.db.dao.ThemeProgressDao
import com.kapoue.agora.data.local.db.entity.MultiplayerSessionEntity
import com.kapoue.agora.data.local.db.entity.ProgressEntity
import com.kapoue.agora.data.local.db.entity.QuestionEntity
import com.kapoue.agora.data.local.db.entity.ThemeProgressEntity

@Database(
    entities = [QuestionEntity::class, ProgressEntity::class, ThemeProgressEntity::class, MultiplayerSessionEntity::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AgoraDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun progressDao(): ProgressDao
    abstract fun themeProgressDao(): ThemeProgressDao
    abstract fun multiplayerSessionDao(): MultiplayerSessionDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS multiplayer_sessions (" +
                    "sessionId TEXT NOT NULL, " +
                    "organizerName TEXT NOT NULL, " +
                    "totalRounds INTEGER NOT NULL, " +
                    "questionsPerRound INTEGER NOT NULL, " +
                    "currentRound INTEGER NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "resultsJson TEXT NOT NULL, " +
                    "eliminatedJson TEXT NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(sessionId))"
                )
            }
        }

        // v4 → v5 : ajout colonne explanation (explication de la bonne réponse)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE questions ADD COLUMN explanation TEXT DEFAULT NULL")
            }
        }

        // v3 → v4 : dédoublonnage de la table questions
        // Des doublons se sont accumulés au fil des mises à jour (même questionText,
        // même theme, même difficulty, IDs différents).
        // On conserve l'état "répondu" si au moins un doublon l'était,
        // puis on supprime toutes les copies en gardant uniquement le MIN(id).
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Étape 1 : si un doublon était répondu correctement,
                // on le transfère sur le survivant (MIN id du groupe)
                database.execSQL("""
                    UPDATE questions SET isAnsweredCorrectly = 1
                    WHERE id IN (
                        SELECT MIN(id) FROM questions
                        GROUP BY theme, difficulty, questionText
                        HAVING MAX(isAnsweredCorrectly) = 1
                    )
                """.trimIndent())

                // Étape 2 : supprimer tous les doublons, garder MIN(id) par groupe
                database.execSQL("""
                    DELETE FROM questions WHERE id NOT IN (
                        SELECT MIN(id) FROM questions
                        GROUP BY theme, difficulty, questionText
                    )
                """.trimIndent())
            }
        }
    }
}
