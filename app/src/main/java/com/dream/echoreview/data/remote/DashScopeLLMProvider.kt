package com.dream.echoreview.data.remote

import com.dream.echoreview.data.repository.UserPreferencesRepository
import com.dream.echoreview.domain.repository.LLMProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashScopeLLMProvider @Inject constructor(
    private val api: DashScopeApi,
    private val preferencesRepository: UserPreferencesRepository
) : LLMProvider {

    override val modelName: String = "DashScope"

    override fun generateSummaryStream(transcript: String): Flow<String> = flow {
        try {
            val key = preferencesRepository.dashScopeApiKeyFlow.first()
                ?: throw Exception("请先在设置中配置 API Key")
            
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
                // 模拟流式输出
                content.forEach {
                    emit(it.toString())
                    kotlinx.coroutines.delay(5)
                }
            } else {
                emit("AI 返回内容为空")
            }
        } catch (e: Exception) {
            emit("生成总结出错: ${e.message}")
        }
    }
}
