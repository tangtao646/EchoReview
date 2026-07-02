package com.dream.echoreview.di

import com.dream.echoreview.data.remote.DashScopeApi
import com.dream.echoreview.data.remote.DeepSeekApi
import com.dream.echoreview.data.remote.GeminiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideDashScopeApi(client: OkHttpClient): DashScopeApi {
        return Retrofit.Builder()
            .baseUrl("https://llm-lin7t0pdieh3zig0.cn-beijing.maas.aliyuncs.com/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DashScopeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDeepSeekApi(client: OkHttpClient): DeepSeekApi {
        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApi(client: OkHttpClient): GeminiApi {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }
}
