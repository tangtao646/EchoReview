package com.dream.echoreview.data.remote

import android.util.Log
import com.dream.echoreview.data.repository.UserPreferencesRepository
import com.dream.echoreview.domain.repository.ISTTEngine
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.*
import okio.ByteString.Companion.toByteString
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashScopeSTTEngine @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val okHttpClient: OkHttpClient
) : ISTTEngine {

    private val TAG = "DashScopeSTTEngine"
    private val gson = Gson()
    private val baseWsUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"

    override suspend fun transcribe(audioFile: File): Result<String> {
        return Result.failure(Exception("请使用流式转写"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun transcribeStream(audioFlow: Flow<ByteArray>): Flow<String> {
        // 使用 Flow 的经典扩展，实现自动重连机制
        return createWebSocketFlow(audioFlow)
            .retry { cause ->
                Log.w(TAG, "连接发生异常，准备自动重连... 异常原因: ${cause.message}")
                delay(2000) // 延迟2秒后尝试重连，防止高频死循环
                true // 返回 true 代表愿意继续重连
            }
    }

    private fun createWebSocketFlow(audioFlow: Flow<ByteArray>): Flow<String> = callbackFlow {
        Log.d(TAG, "开始准备流式转写连接...")

        val apiKey = preferencesRepository.apiKeyFlow.firstOrNull()
        if (apiKey.isNullOrBlank()) {
            close(Exception("错误: 请先在设置中配置 API Key"))
            return@callbackFlow
        }

        val request = Request.Builder()
            .url(baseWsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        var webSocket: WebSocket? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket 已打开: ${response.code} ${response.message}")

                // 💡 彻底修复：构建完整且符合百炼协议的 run-task 报文
                val startMessage = JsonObject().apply {
                    val header = JsonObject().apply {
                        addProperty("action", "run-task")
                        addProperty("task_id", java.util.UUID.randomUUID().toString().replace("-", ""))
                        addProperty("streaming", "duplex")
                    }
                    val payload = JsonObject().apply {
                        addProperty("task_group", "audio")
                        addProperty("task", "asr")
                        addProperty("function", "recognition")
                        addProperty("model", "paraformer-realtime-v1")

                        // 核心修复点：必须加上空对象的 input 参数占位
                        add("input", JsonObject())

                        val parameters = JsonObject().apply {
                            addProperty("format", "pcm")
                            addProperty("sample_rate", 16000)
                        }
                        add("parameters", parameters)
                    }
                    add("header", header)
                    add("payload", payload)
                }

                val jsonStr = gson.toJson(startMessage)
                Log.d(TAG, "发送首帧握手配置: $jsonStr")
                webSocket.send(jsonStr)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "收到消息: $text")
                try {
                    val response = gson.fromJson(text, JsonObject::class.java)
                    if (response.has("header")) {
                        val header = response.getAsJsonObject("header")
                        val action = header?.get("action")?.asString

                        when (action) {
                            "result-generated" -> {
                                val payload = response.getAsJsonObject("payload")
                                val output = payload?.getAsJsonObject("output")
                                val sentence = output?.getAsJsonObject("sentence")
                                val transcription = sentence?.get("text")?.asString
                                if (!transcription.isNullOrBlank()) {
                                    trySend(transcription)
                                }
                            }
                            "task-failed" -> {
                                val errMsg = header?.get("error_message")?.asString ?: "未知错误"
                                Log.e(TAG, "阿里服务端任务失败: $errMsg")
                                close(Exception("服务端任务失败: $errMsg"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "消息解析异常: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket 底层连接失败/断开: ${t.message}")
                // 抛出异常以触发上层 Flow 的 .retry{} 重连机制
                close(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 正在关闭: $reason")
                close()
            }
        }

        webSocket = okHttpClient.newWebSocket(request, listener)

        // 启动独立协程，将 audioFlow 的字节无脑泵入 WebSocket
        val audioJob = launch(Dispatchers.IO) {
            try {
                audioFlow.collect { chunk ->
                    webSocket.send(chunk.toByteString())
                }

                Log.d(TAG, "最上层音频流供给结束，发送 finish-task 报文...")
                val stopMessage = JsonObject().apply {
                    val header = JsonObject().apply {
                        addProperty("action", "finish-task")
                        addProperty("task_id", java.util.UUID.randomUUID().toString().replace("-", ""))
                    }
                    add("header", header)
                    add("payload", JsonObject())
                }
                webSocket.send(gson.toJson(stopMessage))
            } catch (e: Exception) {
                Log.e(TAG, "音频流发送协程发生中断: ${e.message}")
            }
        }

        awaitClose {
            Log.d(TAG, "正在清理流资源，彻底断开当前 WebSocket")
            audioJob.cancel()
            webSocket.close(1000, "User Disconnect")
        }
    }
}