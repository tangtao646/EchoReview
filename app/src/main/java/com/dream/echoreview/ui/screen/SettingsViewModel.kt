package com.dream.echoreview.ui.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.R
import com.dream.echoreview.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
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
            _saveResult.emit(context.getString(R.string.dashscope_key_saved))
        }
    }

    fun updateDeepSeekKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateDeepSeekKey(key)
            _saveResult.emit(context.getString(R.string.deepseek_key_saved))
        }
    }

    fun updateGeminiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateGeminiKey(key)
            _saveResult.emit(context.getString(R.string.gemini_key_saved))
        }
    }

    fun updateSelectedModel(model: String) {
        viewModelScope.launch {
            preferencesRepository.updateSelectedModel(model)
            _saveResult.emit(context.getString(R.string.model_switched, model))
        }
    }
}
