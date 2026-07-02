package com.dream.echoreview.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.data.repository.AIProvider
import com.dream.echoreview.domain.model.InterviewSession
import com.dream.echoreview.domain.repository.IAudioPlayer
import com.dream.echoreview.domain.repository.IInterviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: IInterviewRepository,
    private val aiProvider: AIProvider,
    private val audioPlayer: IAudioPlayer
) : ViewModel() {

    val isPlaying = audioPlayer.isPlaying
    val currentPosition = audioPlayer.currentPosition
    val duration = audioPlayer.duration

    private val _session = MutableStateFlow<InterviewSession?>(null)
    val session = _session.asStateFlow()

    private val _aiSummary = MutableStateFlow("")
    val aiSummary = _aiSummary.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadSession(id: String) {
        viewModelScope.launch {
            val s = repository.getSessionById(id)
            _session.value = s
            _aiSummary.value = s?.aiSummary ?: ""
        }
    }

    fun togglePlayback() {
        val currentSession = _session.value ?: return
        if (isPlaying.value) {
            audioPlayer.pause()
        } else {
            audioPlayer.play(File(currentSession.audioPath))
        }
    }

    fun seekTo(position: Long) {
        audioPlayer.seekTo(position)
    }

    fun generateAIReview() {
        val currentSession = _session.value ?: return
        val transcript = currentSession.transcript ?: "暂无转录内容"
        
        viewModelScope.launch {
            _isGenerating.value = true
            _aiSummary.value = "" // 重置总结内容用于流式展示
            
            try {
                val aiServiceResult = aiProvider.getCurrentService()
                val aiService = aiServiceResult.getOrElse { throw it }
                
                aiService.generateSummaryStream(transcript).collect { chunk ->
                    _aiSummary.value += chunk
                }
                
                // 完成后更新数据库并同步当前 Session 状态
                repository.updateResults(currentSession.id, transcript, _aiSummary.value)
                _session.value = _session.value?.copy(aiSummary = _aiSummary.value)
            } catch (e: Exception) {
                _error.value = "总结失败: ${e.localizedMessage}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
