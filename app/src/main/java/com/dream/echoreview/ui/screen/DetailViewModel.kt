package com.dream.echoreview.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.domain.model.InterviewSession
import com.dream.echoreview.domain.repository.IInterviewRepository
import com.dream.echoreview.domain.repository.ILLMProvider
import com.dream.echoreview.domain.repository.ISTTEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

import com.dream.echoreview.domain.repository.IAudioPlayer
// ...
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: IInterviewRepository,
    private val sttEngine: ISTTEngine,
    private val llmProvider: ILLMProvider,
    private val audioPlayer: IAudioPlayer
) : ViewModel() {

    val isPlaying = audioPlayer.isPlaying
    val currentPosition = audioPlayer.currentPosition
    val duration = audioPlayer.duration

    private val _session = MutableStateFlow<InterviewSession?>(null)
// ...
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

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
    val session = _session.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadSession(id: String) {
        viewModelScope.launch {
            _session.value = repository.getSessionById(id)
        }
    }

    fun generateAIReview() {
        val currentSession = _session.value ?: return
        
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // 1. STT
                val audioFile = File(currentSession.audioPath)
                val transcriptionResult = sttEngine.transcribe(audioFile)
                val transcript = transcriptionResult.getOrThrow()

                // 2. LLM Summary
                val summaryResult = llmProvider.generateSummary(transcript)
                val summary = summaryResult.getOrThrow()

                // 3. Update Local DB
                repository.updateResults(currentSession.id, transcript, summary)
                
                // 4. Reload
                loadSession(currentSession.id)
            } catch (e: Exception) {
                _error.value = "生成失败: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
