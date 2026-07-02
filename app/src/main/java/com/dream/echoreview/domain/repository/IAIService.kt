package com.dream.echoreview.domain.repository

import com.dream.echoreview.domain.model.StreamSegment
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ISTTEngine {
    suspend fun transcribe(audioFile: File): Result<String>
    // 升级：由原始字符串流转为领域模型流
    fun transcribeStream(audioFlow: Flow<ByteArray>): Flow<StreamSegment>
}

interface ILLMProvider {
    suspend fun generateSummary(transcript: String): Result<String>
}
