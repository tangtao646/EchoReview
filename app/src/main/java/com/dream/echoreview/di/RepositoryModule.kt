package com.dream.echoreview.di

import com.dream.echoreview.data.repository.InterviewRepositoryImpl
import com.dream.echoreview.domain.repository.IInterviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindInterviewRepository(
        impl: InterviewRepositoryImpl
    ): IInterviewRepository
}
