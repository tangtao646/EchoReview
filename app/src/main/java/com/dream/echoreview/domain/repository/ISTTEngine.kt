package com.dream.echoreview.domain.repository

import com.dream.echoreview.domain.model.StreamSegment
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * 语音转文字 (STT) 引擎接口
 */
interface ISTTEngine {
    /**
     * 对整个音频文件进行转录
     */
    suspend fun transcribe(audioFile: File): Result<String>

    /**
     * 流式转录音频数据
     */
    fun transcribeStream(audioFlow: Flow<ByteArray>): Flow<StreamSegment>
}
