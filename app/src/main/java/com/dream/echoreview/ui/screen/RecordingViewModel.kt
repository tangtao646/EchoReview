package com.dream.echoreview.ui.screen

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.data.audio.RecordingService
import com.dream.echoreview.domain.model.InterviewSession
import com.dream.echoreview.domain.repository.IAudioRecorder
import com.dream.echoreview.domain.repository.IInterviewRepository
import com.dream.echoreview.domain.repository.ISTTEngine
import com.dream.echoreview.domain.repository.RecordingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val audioRecorder: IAudioRecorder,
    private val repository: IInterviewRepository,
    private val sttEngine: ISTTEngine,
    private val app: Application
) : ViewModel() {

    val recordingState: StateFlow<RecordingState> = audioRecorder.stateFlow

    val amplitude: StateFlow<Float> = audioRecorder.amplitudeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val formattedDuration: StateFlow<String> = audioRecorder.durationMillis
        .map { ms ->
            val seconds = (ms / 1000) % 60
            val minutes = (ms / (1000 * 60)) % 60
            val hours = (ms / (1000 * 60 * 60))
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00:00")

    private val _companyName = MutableStateFlow("")
    val companyName = _companyName.asStateFlow()

    private val _realtimeTranscript = MutableStateFlow("")
    val realtimeTranscript = _realtimeTranscript.asStateFlow()

    private var currentFile: File? = null
    private var currentId: String = ""
    private var sttJob: Job? = null

    fun updateCompanyName(name: String) {
        _companyName.value = name
    }

    fun startRecording() {
        currentId = UUID.randomUUID().toString()
        val file = File(app.filesDir, "recordings/${currentId}.m4a")
        file.parentFile?.mkdirs()
        currentFile = file
        
        // 1. 启动实时转写 (恢复为真实的麦克风音频流)
        sttJob?.cancel()
        sttJob = viewModelScope.launch {
            sttEngine.transcribeStream(audioRecorder.audioFlow)
                .onStart { _realtimeTranscript.value = "正在连接 AI 转写..." }
                .catch { e -> _realtimeTranscript.value = "转写连接失败: ${e.message}" }
                .collect { text ->
                    _realtimeTranscript.value = text
                }
        }

        // 2. 启动录音服务
        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra("file_path", file.absolutePath)
        }
        app.startForegroundService(intent)
        
        // 3. 预存 Session
        viewModelScope.launch {
            repository.insertSession(
                InterviewSession(
                    id = currentId,
                    companyName = _companyName.value,
                    audioPath = file.absolutePath,
                    transcript = null,
                    aiSummary = null,
                    timestamp = System.currentTimeMillis(),
                    tags = emptyList()
                )
            )
        }
    }

    fun stopRecording() {
        sttJob?.cancel()
        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        app.startService(intent)
        
        // 保存最终文字
        val finalTranscript = _realtimeTranscript.value
        viewModelScope.launch {
            repository.updateResults(currentId, finalTranscript, "")
        }
    }
}
