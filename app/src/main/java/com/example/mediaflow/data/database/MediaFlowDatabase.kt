package com.example.mediaflow.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mediaflow.data.database.dao.DownloadHistoryDao
import com.example.mediaflow.data.database.entity.DownloadHistoryEntity

@Database(
    entities = [DownloadHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MediaFlowDatabase : RoomDatabase() {

    abstract fun downloadHistoryDao(): DownloadHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MediaFlowDatabase? = null

        fun getInstance(context: Context): MediaFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediaFlowDatabase::class.java,
                    "mediaflow.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
