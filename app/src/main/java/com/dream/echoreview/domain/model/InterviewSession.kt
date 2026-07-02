package com.dream.echoreview.domain.model

data class InterviewSession(
    val id: String,
    val companyName: String,
    val audioPath: String,
    val transcript: String?,
    val aiSummary: String?,
    val timestamp: Long,
    val tags: List<String>,
    val aiModelUsed: String? = null,
    val isLocalInference: Boolean = false
)
