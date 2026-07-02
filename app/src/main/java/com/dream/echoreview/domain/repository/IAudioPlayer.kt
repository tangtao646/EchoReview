package com.dream.echoreview.domain.repository

import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface IAudioPlayer {
    fun play(file: File)
    fun pause()
    fun stop()
    fun seekTo(position: Long)
    
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
}
