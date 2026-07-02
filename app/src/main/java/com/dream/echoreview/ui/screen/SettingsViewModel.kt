package com.dream.echoreview.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

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
        }
    }

    fun updateDeepSeekKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateDeepSeekKey(key)
        }
    }

    fun updateGeminiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateGeminiKey(key)
        }
    }

    fun updateSelectedModel(model: String) {
        viewModelScope.launch {
            preferencesRepository.updateSelectedModel(model)
        }
    }
}
