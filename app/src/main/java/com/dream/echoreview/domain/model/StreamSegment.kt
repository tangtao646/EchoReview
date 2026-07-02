package com.dream.echoreview.domain.model

/**
 * 流式语音转写段落模型
 */
data class StreamSegment(
    val text: String,       // 当前句子的全量文本内容
    val isFinal: Boolean,   // 该句子是否已完结 (触发切句)
    val beginTime: Long     // 该句子的起始时间 (毫秒)
)
