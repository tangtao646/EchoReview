package com.dream.echoreview.ui.screen

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.echoreview.R
import com.dream.echoreview.data.audio.RecordingService
import com.dream.echoreview.data.repository.UserPreferencesRepository
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


sealed class RecordingUiEvent {
    object ShowApiKeyMissingDialog : RecordingUiEvent()
    data class NavigateToDetail(val sessionId: String) : RecordingUiEvent()
}
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val audioRecorder: IAudioRecorder,
    private val repository: IInterviewRepository,
    private val sttEngine: ISTTEngine,
    private val preferencesRepository: UserPreferencesRepository,
    private val app: Application
) : ViewModel() {

    val isApiKeyValid: StateFlow<Boolean?> = combine(
        preferencesRepository.dashScopeApiKeyFlow,
        preferencesRepository.deepSeekApiKeyFlow,
        preferencesRepository.geminiApiKeyFlow
    ) {  dashScopeKey, deepSeekKey, geminiKey ->
        // 核心校验逻辑：只校验当前选中的模型，或者全量校验。
        // 这里基于你的业务，转录依赖 DashScope（阿里百炼），后续 AI 总结依赖另外两个[cite: 3, 5]
        // 建议实行严密校验：转录所必需的 DashScope 必须存在[cite: 3]
        !dashScopeKey.isNullOrBlank() && !deepSeekKey.isNullOrBlank()
        // 如果想顺便把当前选中的大模型 Key 也一起拦截了，可以解开下面这行：
        // && (if (model == "DeepSeek") !deepSeekKey.isNullOrBlank() else !geminiKey.isNullOrBlank())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 用于向 UI 抛出一次性弹窗事件通道
    private val _uiEvent = MutableSharedFlow<RecordingUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()
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
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            app.getString(R.string.zero_time)
        )

    private val _companyName = MutableStateFlow("")
    val companyName = _companyName.asStateFlow()

    private val _interviewStage = MutableStateFlow(app.getString(R.string.stage_initial))
    val interviewStage = _interviewStage.asStateFlow()

    private val _realtimeTranscript = MutableStateFlow("")
    val realtimeTranscript = _realtimeTranscript.asStateFlow()

    // 内部维护的状态容器，实现"句内覆盖、句间追加"
    private val finishedSentences = mutableListOf<String>()
    private var currentPartialText = ""

    private var currentFile: File? = null
    private var currentId: String = ""
    private var sttJob: Job? = null

    fun updateCompanyName(name: String) {
        _companyName.value = name
    }

    fun updateInterviewStage(stage: String) {
        _interviewStage.value = stage
    }

    fun startRecording() {

        viewModelScope.launch {
            // 如果正在加载中 (null)，我们利用 first() 强行等待它产出第一个非空的确定结果
            val isValid = if (isApiKeyValid.value == null) {
                isApiKeyValid.filterNotNull().first()
            } else {
                isApiKeyValid.value == true
            }

            if (!isValid) {
                _uiEvent.emit(RecordingUiEvent.ShowApiKeyMissingDialog)
                return@launch // 强行阻断
            }

            // 3. 校验通过，转到主线程或其他指定调度器安全执行原有的录制逻辑
            withContext(Dispatchers.Main) {
                executeStartRecording()
            }
        }
    }

    private fun executeStartRecording() {
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
                .onStart { _realtimeTranscript.value = app.getString(R.string.listening) }
                .catch { e ->
                    Log.e("RecordingVM", "STT Error: ${e.message}")
                    _realtimeTranscript.value =
                        app.getString(R.string.stt_connection_failed, e.message ?: "")
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
                        _realtimeTranscript.emit(fullTranscript)
                    }

                }
        }

        // 2. 启动录音服务
        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra("file_path", file.absolutePath)
        }
        app.startForegroundService(intent)

        // 3. 预存 Session，包含 Stage
        viewModelScope.launch {
            repository.insertSession(
                InterviewSession(
                    id = currentId,
                    companyName = _companyName.value,
                    interviewStage = _interviewStage.value,
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
        val finalDuration = audioRecorder.durationMillis.value // 获取最终时长
        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        app.startService(intent)

        // 保存最终文字和时长
        val listeningHolder = app.getString(R.string.listening)
        val emptyTesHolder = app.getString(R.string.empty_text_placeholder)
        val finalTranscript =
            if (_realtimeTranscript.value == listeningHolder) emptyTesHolder else _realtimeTranscript.value
        viewModelScope.launch {
            repository.updateResults(currentId, finalTranscript, "", finalDuration)
            _uiEvent.emit(RecordingUiEvent.NavigateToDetail(currentId))
        }
    }

    fun discardRecording() {
        sttJob?.cancel()
        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        app.startService(intent)

        viewModelScope.launch {
            // 删除数据库记录
            repository.deleteSession(currentId)
            // 删除本地文件
            currentFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
    }
}
