package com.dream.echoreview.data.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.dream.echoreview.domain.repository.IAudioPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) : IAudioPlayer {

    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null
    private var currentFile: File? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration = _duration.asStateFlow()

    private fun initPlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) startProgressUpdate() else stopProgressUpdate()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            // 确保 duration 是有效值（毫秒）
                            _duration.value = if (duration > 0) duration else 0L
                        } else if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                            _currentPosition.value = _duration.value
                            stopProgressUpdate()
                        }
                    }
                })
            }
        }
    }

    override fun play(file: File) {
        initPlayer()
        if (currentFile?.absolutePath == file.absolutePath && player?.playbackState != Player.STATE_IDLE) {
            // 如果文件相同且已经准备好，直接播放（实现继续播放逻辑）
            player?.play()
        } else {
            // 如果是新文件，则重新加载
            currentFile = file
            player?.apply {
                val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                setMediaItem(mediaItem)
                prepare()
                play()
            }
        }
    }

    override fun pause() {
        player?.pause()
    }

    override fun stop() {
        player?.stop()
        currentFile = null
        stopProgressUpdate()
    }

    override fun seekTo(position: Long) {
        player?.seekTo(position)
        _currentPosition.value = position
    }

    private fun startProgressUpdate() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                val pos = player?.currentPosition ?: 0L
                _currentPosition.value = pos
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        job?.cancel()
    }
}
