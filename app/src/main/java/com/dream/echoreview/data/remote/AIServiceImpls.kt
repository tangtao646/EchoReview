package com.dream.echoreview.data.remote

import android.content.Context
import com.dream.echoreview.R
import com.dream.echoreview.domain.repository.LLMProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeepSeekServiceImpl(
    private val api: DeepSeekApi,
    private val apiKey: String,
    private val context: Context? = null
) : LLMProvider {
    override val modelName = "DeepSeek"

    override fun generateSummaryStream(transcript: String): Flow<String> = flow {
        try {
            val response = api.chatCompletion(
                auth = "Bearer $apiKey",
                request = ChatRequest(
                    model = "deepseek-chat",
                    messages = listOf(
                        ChatMessage("system", context?.getString(R.string.ai_prompt_deepseek) ?: "你是一个专业的面试总结助手。请对以下面试内容进行精简、客观的总结，突出关键点。"),
                        ChatMessage("user", transcript)
                    )
                )
            )
            val summary = response.choices.firstOrNull()?.message?.content
            summary?.forEach {
                emit(it.toString())
                kotlinx.coroutines.delay(10) // 微小延迟模拟打字机
            }
        } catch (e: Exception) {
            emit(context?.getString(R.string.summary_generation_error, e.message ?: "") ?: "总结生成出错: ${e.message}")
        }
    }
}

class GeminiServiceImpl(
    private val api: GeminiApi,
    private val apiKey: String,
    private val context: Context? = null
) : LLMProvider {
    override val modelName = "Gemini"

    override fun generateSummaryStream(transcript: String): Flow<String> = flow {
        try {
            val promptPrefix = context?.getString(R.string.ai_prompt_gemini) ?: "请对以下面试内容进行精简、客观的总结："
            val response = api.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = "$promptPrefix\n\n$transcript")))
                    )
                )
            )
            val summary = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            summary?.forEach {
                emit(it.toString())
                kotlinx.coroutines.delay(10)
            }
        } catch (e: Exception) {
            emit(context?.getString(R.string.summary_generation_error, e.message ?: "") ?: "总结生成出错: ${e.message}")
        }
    }
}
