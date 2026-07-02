package com.dream.echoreview.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface IAudioRecorder {
    fun start(outputFile: File)
    fun stop()
    fun pause()
    fun resume()
    val audioFlow: Flow<ByteArray>
    val stateFlow: StateFlow<RecordingState>
    val durationMillis: StateFlow<Long>
    val amplitudeFlow: Flow<Float> // 0.0 to 1.0
}

enum class RecordingState {
    IDLE, RECORDING, PAUSED, STOPPED
}
