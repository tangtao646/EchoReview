package com.dream.echoreview.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * AI 服务抽象接口
 */
interface LLMProvider {
    val modelName: String

    /**
     * 根据面试转录内容生成总结（流式）
     * @param transcript 面试原始文本
     * @return 总结内容的流
     */
    fun generateSummaryStream(transcript: String): Flow<String>
}
