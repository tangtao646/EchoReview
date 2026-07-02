package com.dream.echoreview.data.audio

import android.annotation.SuppressLint
import android.media.*
import android.util.Log
import com.dream.echoreview.domain.repository.IAudioRecorder
import com.dream.echoreview.domain.repository.RecordingState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAudioRecorder @Inject constructor() : IAudioRecorder {
    
    private var audioRecord: AudioRecord? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var trackIndex = -1
    private var isMuxerStarted = false
    
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var startTimeNano = 0L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _audioFlow = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST //  网络卡顿时，丢弃老音频数据，绝对不卡死麦克风采集协程
    )
    override val audioFlow = _audioFlow.asSharedFlow()

    private val _stateFlow = MutableStateFlow(RecordingState.IDLE)
    override val stateFlow = _stateFlow.asStateFlow()

    private val _durationMillis = MutableStateFlow(0L)
    override val durationMillis = _durationMillis.asStateFlow()

    private val _amplitudeFlow = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    override val amplitudeFlow = _amplitudeFlow.asSharedFlow()

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val TAG = "AndroidAudioRecorder"

    @SuppressLint("MissingPermission")
    override fun start(outputFile: File) {
        if (_stateFlow.value == RecordingState.RECORDING) return

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, channelConfig, audioFormat, minBufferSize)

        setupCodec(outputFile)
        audioRecord?.startRecording()
        
        startTimeNano = System.nanoTime()
        _stateFlow.value = RecordingState.RECORDING
        _durationMillis.value = 0L
        startTimer()

        recordingJob = scope.launch {
            val buffer = ByteArray(minBufferSize)
            while (isActive && _stateFlow.value != RecordingState.STOPPED) {
                if (_stateFlow.value == RecordingState.PAUSED) {
                    delay(100)
                    continue
                }

                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val data = buffer.copyOfRange(0, read)
                    
                    // 1. 发射原始 PCM 流给 AI
                    _audioFlow.emit(data)
                    
                    // 2. 计算音量振幅
                    calculateAmplitude(data)

                    // 3. 安全编码并保存 (修复溢出问题)
                    encodeSafe(data)
                }
            }
            releaseCodec()
        }
    }

    private fun calculateAmplitude(data: ByteArray): Float {
        var sum = 0.0
        for (i in data.indices step 2) {
            if (i + 1 < data.size) {
                val sample = ByteBuffer.wrap(data, i, 2).order(ByteOrder.LITTLE_ENDIAN).short.toDouble()
                sum += sample * sample
            }
        }
        val rms = Math.sqrt(sum / (data.size / 2))
        val db = if (rms > 0) 20 * Math.log10(rms) else 0.0
        val normalized = ((db - 30) / (80 - 30)).coerceIn(0.0, 1.0).toFloat()
        scope.launch { _amplitudeFlow.emit(normalized) }
        return normalized
    }

    private fun setupCodec(outputFile: File) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 10)

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()
        mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    // 修复的核心：分段喂数据给 MediaCodec
    private fun encodeSafe(data: ByteArray) {
        val codec = mediaCodec ?: return
        var offset = 0
        while (offset < data.size) {
            val inputBufferIndex = codec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: break
                val remaining = inputBuffer.capacity()
                val length = Math.min(data.size - offset, remaining)
                
                inputBuffer.clear()
                inputBuffer.put(data, offset, length)
                
                val pts = (System.nanoTime() - startTimeNano) / 1000
                codec.queueInputBuffer(inputBufferIndex, 0, length, pts, 0)
                offset += length
            } else {
                // 如果编码器忙，先处理输出再继续
                processOutput()
            }
        }
        processOutput()
    }

    private fun processOutput() {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        
        var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
        while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = muxer.addTrack(codec.outputFormat)
                muxer.start()
                isMuxerStarted = true
            } else if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && isMuxerStarted && trackIndex >= 0) {
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
            }
            outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive && _stateFlow.value != RecordingState.STOPPED) {
                if (_stateFlow.value == RecordingState.RECORDING) {
                    val now = System.currentTimeMillis()
                    _durationMillis.value += (now - lastTime)
                    lastTime = now
                } else {
                    lastTime = System.currentTimeMillis()
                }
                delay(100)
            }
        }
    }

    private fun releaseCodec() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            if (isMuxerStarted) mediaMuxer?.stop()
            mediaMuxer?.release()
            mediaMuxer = null
            trackIndex = -1
            isMuxerStarted = false
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun stop() {
        _stateFlow.value = RecordingState.STOPPED
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) stop()
            release()
        }
        audioRecord = null
        _stateFlow.value = RecordingState.IDLE
    }

    override fun pause() { if (_stateFlow.value == RecordingState.RECORDING) _stateFlow.value = RecordingState.PAUSED }
    override fun resume() { if (_stateFlow.value == RecordingState.PAUSED) _stateFlow.value = RecordingState.RECORDING }
}
