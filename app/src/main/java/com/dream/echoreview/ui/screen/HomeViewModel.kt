package com.dream.echoreview.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.domain.model.InterviewSession
import com.dream.echoreview.domain.repository.IInterviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IInterviewRepository
) : ViewModel() {

    val sessions: StateFlow<List<InterviewSession>> = repository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalDurationText: StateFlow<String> = sessions.map { list ->
        val totalMs = list.sumOf { it.durationMillis }
        val totalMinutes = totalMs / (1000 * 60)
        "$totalMinutes 分钟"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0 分钟")

    val completionRate: StateFlow<Float> = sessions.map { list ->
        if (list.isEmpty()) 0f
        else list.count { !it.aiSummary.isNullOrEmpty() }.toFloat() / list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun deleteSession(id: String) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }
}
