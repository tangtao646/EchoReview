package com.dream.echoreview.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dream.echoreview.domain.model.InterviewSession

@Entity(tableName = "interview_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val companyName: String,
    val interviewStage: String = "初试",
    val audioPath: String,
    val durationMillis: Long = 0,
    val transcript: String?,
    val aiSummary: String?,
    val timestamp: Long,
    val tags: String, // Stored as comma-separated or JSON string
    val aiModelUsed: String?,
    val isLocalInference: Boolean,
    val rating: Int = 0
)

fun SessionEntity.toDomain(): InterviewSession {
    return InterviewSession(
        id = id,
        companyName = companyName,
        interviewStage = interviewStage,
        audioPath = audioPath,
        durationMillis = durationMillis,
        transcript = transcript,
        aiSummary = aiSummary,
        timestamp = timestamp,
        tags = if (tags.isEmpty()) emptyList() else tags.split(","),
        aiModelUsed = aiModelUsed,
        isLocalInference = isLocalInference,
        rating = rating
    )
}

fun InterviewSession.toEntity(): SessionEntity {
    return SessionEntity(
        id = id,
        companyName = companyName,
        interviewStage = interviewStage,
        audioPath = audioPath,
        durationMillis = durationMillis,
        transcript = transcript,
        aiSummary = aiSummary,
        timestamp = timestamp,
        tags = tags.joinToString(","),
        aiModelUsed = aiModelUsed,
        isLocalInference = isLocalInference,
        rating = rating
    )
}
