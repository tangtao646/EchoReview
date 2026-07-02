package com.dream.echoreview.domain.usecase

import com.dream.echoreview.data.repository.AIProvider
import com.dream.echoreview.domain.repository.IInterviewRepository
import kotlinx.coroutines.flow.fold
import javax.inject.Inject

class GenerateInterviewSummaryUseCase @Inject constructor(
    private val aiProvider: AIProvider,
    private val repository: IInterviewRepository
) {
    suspend operator fun invoke(sessionId: String, transcript: String): Result<String> {
        return try {
            // 1. 获取当前选中的 AI 服务
            val aiServiceResult = aiProvider.getCurrentService()
            val aiService = aiServiceResult.getOrElse { throw it }
            
            // 2. 调用 AI 生成总结（流式合并成完整文本）
            val fullSummary = aiService.generateSummaryStream(transcript).fold("") { acc, chunk ->
                acc + chunk
            }
            
            // 3. 将结果更新到本地数据库
            repository.updateResults(sessionId, transcript, fullSummary)
            
            Result.success(fullSummary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
