package com.dream.echoreview.domain.model

data class InterviewSession(
    val id: String,
    val companyName: String,
    val interviewStage: String = "初试",
    val audioPath: String,
    val durationMillis: Long = 0, // 新增时长字段
    val transcript: String?,
    val aiSummary: String?,
    val timestamp: Long,
    val tags: List<String>,
    val rating: Int = 0,
    val aiModelUsed: String? = null,
    val isLocalInference: Boolean = false
)
