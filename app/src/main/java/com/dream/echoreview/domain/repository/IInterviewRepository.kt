package com.dream.echoreview.domain.repository

import com.dream.echoreview.domain.model.InterviewSession
import kotlinx.coroutines.flow.Flow

interface IInterviewRepository {
    fun getAllSessions(): Flow<List<InterviewSession>>
    suspend fun getSessionById(id: String): InterviewSession?
    suspend fun insertSession(session: InterviewSession)
    suspend fun deleteSession(id: String)
    suspend fun updateResults(id: String, transcript: String, aiSummary: String)
}
