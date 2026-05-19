package com.kapoue.agora.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MultiplayerSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: com.kapoue.agora.data.local.db.entity.MultiplayerSessionEntity)

    @Update
    suspend fun updateSession(session: com.kapoue.agora.data.local.db.entity.MultiplayerSessionEntity)

    @Query("SELECT * FROM multiplayer_sessions WHERE status = 'IN_PROGRESS' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveSession(): com.kapoue.agora.data.local.db.entity.MultiplayerSessionEntity?

    @Query("SELECT * FROM multiplayer_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): com.kapoue.agora.data.local.db.entity.MultiplayerSessionEntity?

    @Query("UPDATE multiplayer_sessions SET status = 'COMPLETED' WHERE sessionId = :sessionId")
    suspend fun completeSession(sessionId: String)

    @Query("DELETE FROM multiplayer_sessions WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedSessions()
}
