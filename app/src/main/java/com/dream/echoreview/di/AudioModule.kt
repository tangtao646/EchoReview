package com.dream.echoreview.di

import com.dream.echoreview.data.audio.AndroidAudioPlayer
import com.dream.echoreview.data.audio.AndroidAudioRecorder
import com.dream.echoreview.domain.repository.IAudioPlayer
import com.dream.echoreview.domain.repository.IAudioRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(
        impl: AndroidAudioRecorder
    ): IAudioRecorder

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(
        impl: AndroidAudioPlayer
    ): IAudioPlayer
}
