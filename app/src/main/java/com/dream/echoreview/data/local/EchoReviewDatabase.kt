package com.dream.echoreview.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class EchoReviewDatabase : RoomDatabase() {
    abstract val sessionDao: SessionDao

    companion object {
        const val DATABASE_NAME = "echo_review_db"
    }
}
