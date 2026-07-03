package com.dream.echoreview.data.repository

import com.dream.echoreview.data.local.SessionDao
import com.dream.echoreview.data.local.toDomain
import com.dream.echoreview.data.local.toEntity
import com.dream.echoreview.domain.model.InterviewSession
import com.dream.echoreview.domain.repository.IInterviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterviewRepositoryImpl @Inject constructor(
    private val dao: SessionDao
) : IInterviewRepository {

    override fun getAllSessions(): Flow<List<InterviewSession>> {
        return dao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSessionById(id: String): InterviewSession? {
        return dao.getSessionById(id)?.toDomain()
    }

    override suspend fun insertSession(session: InterviewSession) {
        dao.insertSession(session.toEntity())
    }

    override suspend fun deleteSession(id: String) {
        dao.deleteSession(id)
    }

    override suspend fun updateResults(id: String, transcript: String, aiSummary: String, durationMillis: Long) {
        dao.updateResults(id, transcript, aiSummary, durationMillis)
    }
}
