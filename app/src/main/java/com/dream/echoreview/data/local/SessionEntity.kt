package com.dream.echoreview.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dream.echoreview.domain.model.InterviewSession

@Entity(tableName = "interview_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val companyName: String,
    val audioPath: String,
    val transcript: String?,
    val aiSummary: String?,
    val timestamp: Long,
    val tags: String, // Stored as comma-separated or JSON string
    val aiModelUsed: String?,
    val isLocalInference: Boolean
)

fun SessionEntity.toDomain(): InterviewSession {
    return InterviewSession(
        id = id,
        companyName = companyName,
        audioPath = audioPath,
        transcript = transcript,
        aiSummary = aiSummary,
        timestamp = timestamp,
        tags = if (tags.isEmpty()) emptyList() else tags.split(","),
        aiModelUsed = aiModelUsed,
        isLocalInference = isLocalInference
    )
}

fun InterviewSession.toEntity(): SessionEntity {
    return SessionEntity(
        id = id,
        companyName = companyName,
        audioPath = audioPath,
        transcript = transcript,
        aiSummary = aiSummary,
        timestamp = timestamp,
        tags = tags.joinToString(","),
        aiModelUsed = aiModelUsed,
        isLocalInference = isLocalInference
    )
}
