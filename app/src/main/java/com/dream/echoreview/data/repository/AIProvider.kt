package com.dream.echoreview.data.repository

import android.content.Context
import com.dream.echoreview.R
import com.dream.echoreview.data.remote.DeepSeekApi
import com.dream.echoreview.data.remote.DeepSeekServiceImpl
import com.dream.echoreview.data.remote.GeminiApi
import com.dream.echoreview.data.remote.GeminiServiceImpl
import com.dream.echoreview.domain.repository.LLMProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIProvider @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    private val deepSeekApi: DeepSeekApi,
    private val geminiApi: GeminiApi,
    @ApplicationContext private val context: Context
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
                    Result.failure(Exception(context.getString(R.string.deepseek_key_not_set)))
                } else {
                    Result.success(DeepSeekServiceImpl(deepSeekApi, deepSeekKey))
                }
            }
            "Gemini" -> {
                if (geminiKey.isNullOrBlank()) {
                    Result.failure(Exception(context.getString(R.string.gemini_key_not_set)))
                } else {
                    Result.success(GeminiServiceImpl(geminiApi, geminiKey))
                }
            }
            else -> Result.failure(Exception(context.getString(R.string.unknown_ai_model, model)))
        }
    }
}
