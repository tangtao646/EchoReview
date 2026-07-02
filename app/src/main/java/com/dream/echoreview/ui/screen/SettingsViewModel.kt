package com.dream.echoreview.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _saveResult = MutableSharedFlow<String>()
    val saveResult = _saveResult.asSharedFlow()

    val dashScopeApiKey: StateFlow<String?> = preferencesRepository.dashScopeApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val deepSeekApiKey: StateFlow<String?> = preferencesRepository.deepSeekApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val geminiApiKey: StateFlow<String?> = preferencesRepository.geminiApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedModel: StateFlow<String> = preferencesRepository.selectedModelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DeepSeek")

    fun updateDashScopeApiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateDashScopeApiKey(key)
            _saveResult.emit("DashScope Key 已保存")
        }
    }

    fun updateDeepSeekKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateDeepSeekKey(key)
            _saveResult.emit("DeepSeek Key 已保存")
        }
    }

    fun updateGeminiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateGeminiKey(key)
            _saveResult.emit("Gemini Key 已保存")
        }
    }

    fun updateSelectedModel(model: String) {
        viewModelScope.launch {
            preferencesRepository.updateSelectedModel(model)
            _saveResult.emit("已切换至模型: $model")
        }
    }
}
