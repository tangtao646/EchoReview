package com.dream.echoreview.data.remote

import com.dream.echoreview.data.repository.UserPreferencesRepository
import com.dream.echoreview.domain.repository.ILLMProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashScopeLLMProvider @Inject constructor(
    private val api: DashScopeApi,
    private val preferencesRepository: UserPreferencesRepository
) : ILLMProvider {

    override suspend fun generateSummary(transcript: String): Result<String> {
        return try {
            val key = preferencesRepository.apiKeyFlow.first()
                ?: return Result.failure(Exception("请先在设置中配置 API Key"))
            
            val authHeader = "Bearer $key"
            val request = QwenRequest(
                input = QwenInput(
                    messages = listOf(
                        QwenMessage(role = "system", content = "你是一个面试复盘助手，请根据面试转录文本总结核心问题、回答表现和改进建议。"),
                        QwenMessage(role = "user", content = transcript)
                    )
                )
            )
            val response = api.generateCompletion(authHeader, request)
            val content = response.output.choices.firstOrNull()?.message?.content
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("AI 返回内容为空"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
