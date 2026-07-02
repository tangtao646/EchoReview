package com.dream.echoreview.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    object Keys {
        val DASHSCOPE_API_KEY = stringPreferencesKey("api_key") // 保持与旧版本一致
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val SELECTED_AI_MODEL = stringPreferencesKey("selected_ai_model")
    }

    val dashScopeApiKeyFlow: Flow<String?> = context.dataStore.data.map { it[Keys.DASHSCOPE_API_KEY] }
    val deepSeekApiKeyFlow: Flow<String?> = context.dataStore.data.map { it[Keys.DEEPSEEK_API_KEY] }
    val geminiApiKeyFlow: Flow<String?> = context.dataStore.data.map { it[Keys.GEMINI_API_KEY] }
    val selectedModelFlow: Flow<String> = context.dataStore.data.map { it[Keys.SELECTED_AI_MODEL] ?: "DeepSeek" }

    suspend fun updateDashScopeApiKey(key: String) {
        context.dataStore.edit { it[Keys.DASHSCOPE_API_KEY] = key }
    }

    suspend fun updateDeepSeekKey(key: String) {
        context.dataStore.edit { it[Keys.DEEPSEEK_API_KEY] = key }
    }

    suspend fun updateGeminiKey(key: String) {
        context.dataStore.edit { it[Keys.GEMINI_API_KEY] = key }
    }

    suspend fun updateSelectedModel(model: String) {
        context.dataStore.edit { it[Keys.SELECTED_AI_MODEL] = model }
    }
}
