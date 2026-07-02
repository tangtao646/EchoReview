package com.dream.echoreview.data.repository

import com.dream.echoreview.data.remote.DeepSeekApi
import com.dream.echoreview.data.remote.DeepSeekServiceImpl
import com.dream.echoreview.data.remote.GeminiApi
import com.dream.echoreview.data.remote.GeminiServiceImpl
import com.dream.echoreview.domain.repository.LLMProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIProvider @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    private val deepSeekApi: DeepSeekApi,
    private val geminiApi: GeminiApi
) {
    /**
     * 根据当前用户配置获取 AI 服务实例
     */
    suspend fun getCurrentService(): Result<LLMProvider> {
        val model = userPrefs.selectedModelFlow.first()
        val deepSeekKey = userPrefs.deepSeekApiKeyFlow.first()
        val geminiKey = userPrefs.geminiApiKeyFlow.first()

        return when (model) {
            "DeepSeek" -> {
                if (deepSeekKey.isNullOrBlank()) {
                    Result.failure(Exception("DeepSeek API Key 未设置"))
                } else {
                    Result.success(DeepSeekServiceImpl(deepSeekApi, deepSeekKey))
                }
            }
            "Gemini" -> {
                if (geminiKey.isNullOrBlank()) {
                    Result.failure(Exception("Gemini API Key 未设置"))
                } else {
                    Result.success(GeminiServiceImpl(geminiApi, geminiKey))
                }
            }
            else -> Result.failure(Exception("未知的 AI 模型: $model"))
        }
    }
}
