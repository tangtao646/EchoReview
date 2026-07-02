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

    val apiKey: StateFlow<String?> = preferencesRepository.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateApiKey(key)
        }
    }
}
