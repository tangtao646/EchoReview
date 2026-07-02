package com.dream.echoreview.ui.screen

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.data.audio.RecordingService
import com.dream.echoreview.domain.model.InterviewSession
import com.dream.echoreview.domain.model.StreamSegment
import com.dream.echoreview.domain.repository.IAudioRecorder
import com.dream.echoreview.domain.repository.IInterviewRepository
import com.dream.echoreview.domain.repository.ISTTEngine
import com.dream.echoreview.domain.repository.RecordingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // 内部维护的状态容器，实现“句内覆盖、句间追加”
    private val finishedSentences = mutableListOf<String>()
    private var currentPartialText = ""

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

        // 重置状态
        finishedSentences.clear()
        currentPartialText = ""
        _realtimeTranscript.value = ""

        // 1. 启动实时转写 (采用领域模型流)
        sttJob?.cancel()
        sttJob = viewModelScope.launch {
            sttEngine.transcribeStream(audioRecorder.audioFlow)
                .flowOn(Dispatchers.IO)
                .onStart { _realtimeTranscript.value = "正在连接 AI 转写..." }
                .catch { e ->
                    Log.e("RecordingVM", "STT Error: ${e.message}")
                    _realtimeTranscript.value = "转写连接失败: ${e.message}"
                }
                .collect { segment ->
                    withContext(Dispatchers.Main) {
                        if (segment.isFinal) {
                            // 句间追加：将完结的句子存入历史，并清空临时缓存
                            finishedSentences.add(segment.text)
                            currentPartialText = ""
                        } else {
                            // 句内覆盖：直接替换当前未完结的临时文本
                            currentPartialText = segment.text
                        }
                        // 最终暴露给 UI 的拼接结果
                        val fullTranscript = finishedSentences.joinToString("") + currentPartialText
                        _realtimeTranscript.value = fullTranscript
                    }

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
