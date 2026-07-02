package com.dream.echoreview.domain.repository

import kotlinx.coroutines.flow.Flow
import java.io.File

interface ISTTEngine {
    suspend fun transcribe(audioFile: File): Result<String>
    // 升级：接收原始音频流，返回识别出的实时文本流
    fun transcribeStream(audioFlow: Flow<ByteArray>): Flow<String>
}

interface ILLMProvider {
    suspend fun generateSummary(transcript: String): Result<String>
}
