package com.dream.echoreview.di

import com.dream.echoreview.data.remote.DashScopeLLMProvider
import com.dream.echoreview.data.remote.DashScopeSTTEngine
import com.dream.echoreview.domain.repository.ISTTEngine
import com.dream.echoreview.domain.repository.LLMProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindLLMProvider(
        impl: DashScopeLLMProvider
    ): LLMProvider

    @Binds
    @Singleton
    abstract fun bindSTTEngine(
        impl: DashScopeSTTEngine
    ): ISTTEngine
}
