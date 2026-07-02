package com.dream.echoreview.di

import android.content.Context
import androidx.room.Room
import com.dream.echoreview.data.local.EchoReviewDatabase
import com.dream.echoreview.data.local.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EchoReviewDatabase {
        return Room.databaseBuilder(
            context,
            EchoReviewDatabase::class.java,
            EchoReviewDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: EchoReviewDatabase): SessionDao {
        return database.sessionDao
    }
}
