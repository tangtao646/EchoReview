package com.dream.echoreview.data.remote

import android.content.Context
import com.dream.echoreview.R
import com.dream.echoreview.data.repository.UserPreferencesRepository
import com.dream.echoreview.domain.repository.LLMProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashScopeLLMProvider @Inject constructor(
    private val api: DashScopeApi,
    private val preferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : LLMProvider {

    override val modelName: String = "DashScope"

    override fun generateSummaryStream(transcript: String): Flow<String> = flow {
        try {
            val key = preferencesRepository.dashScopeApiKeyFlow.first()
                ?: throw Exception(context.getString(R.string.please_configure_api_key))

            val authHeader = "Bearer $key"
            val request = QwenRequest(
                input = QwenInput(
                    messages = listOf(
                        QwenMessage(role = "system", content = context.getString(R.string.ai_prompt_qwen)),
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
                emit(context.getString(R.string.ai_returned_empty))
            }
        } catch (e: Exception) {
            emit(context.getString(R.string.summary_generation_error, e.message ?: ""))
        }
    }
}
