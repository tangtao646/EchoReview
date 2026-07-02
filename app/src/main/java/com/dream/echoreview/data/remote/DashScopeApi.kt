package com.dream.echoreview.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface DashScopeApi {
    
    // ASR Paraformer (Asynchronous Task)
    @POST("services/audio/asr/transcription")
    suspend fun createTranscriptionTask(
        @Header("Authorization") apiKey: String,
        @Body request: TranscriptionRequest
    ): TranscriptionResponse

    @GET("tasks/{task_id}")
    suspend fun getTranscriptionResult(
        @Header("Authorization") apiKey: String,
        @Path("task_id") taskId: String
    ): TranscriptionTaskResponse

    // LLM Qwen
    @POST("services/aigc/text-generation/generation")
    suspend fun generateCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: QwenRequest
    ): QwenResponse

    // 通用 GET，用于下载转录结果 JSON
    @GET
    suspend fun downloadExternalResult(@Url url: String): Map<String, Any>
}

data class TranscriptionRequest(
    val model: String = "paraformer-v2",
    val input: TranscriptionInput,
    val parameters: TranscriptionParameters = TranscriptionParameters()
)

data class TranscriptionInput(
    val file_urls: List<String>
)

data class TranscriptionParameters(
    val language_hints: List<String> = listOf("zh", "en")
)

data class TranscriptionResponse(
    val request_id: String,
    val output: TranscriptionOutput
)

data class TranscriptionOutput(
    val task_id: String,
    val task_status: String
)

data class TranscriptionTaskResponse(
    val request_id: String,
    val output: TranscriptionTaskOutput
)

data class TranscriptionTaskOutput(
    val task_id: String,
    val task_status: String,
    val results: List<TranscriptionResult>?
)

data class TranscriptionResult(
    val transcription_url: String? // Note: Actual text is often in a URL or embedded depending on model
)

// Qwen DTOs
data class QwenRequest(
    val model: String = "qwen-turbo",
    val input: QwenInput,
    val parameters: QwenParameters = QwenParameters()
)

data class QwenInput(
    val messages: List<QwenMessage>
)

data class QwenMessage(
    val role: String,
    val content: String
)

data class QwenParameters(
    val result_format: String = "message"
)

data class QwenResponse(
    val request_id: String,
    val output: QwenOutput,
    val usage: QwenUsage
)

data class QwenOutput(
    val choices: List<QwenChoice>
)

data class QwenChoice(
    val message: QwenMessage,
    val finish_reason: String
)

data class QwenUsage(
    val total_tokens: Int
)
