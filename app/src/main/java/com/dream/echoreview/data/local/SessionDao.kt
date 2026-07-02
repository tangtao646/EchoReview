package com.dream.echoreview.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM interview_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM interview_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("DELETE FROM interview_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)
    
    @Query("UPDATE interview_sessions SET transcript = :transcript, aiSummary = :aiSummary WHERE id = :id")
    suspend fun updateResults(id: String, transcript: String, aiSummary: String)
}
