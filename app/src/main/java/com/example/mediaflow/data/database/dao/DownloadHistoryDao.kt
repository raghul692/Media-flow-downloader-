package com.example.mediaflow.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mediaflow.data.database.entity.DownloadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {

    @Query("SELECT * FROM download_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<DownloadHistoryEntity>>

    @Query("SELECT * FROM download_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadHistoryEntity?

    @Query("SELECT * FROM download_history WHERE status IN ('QUEUED', 'ANALYZING', 'DOWNLOADING', 'PROCESSING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadHistoryEntity>>

    @Query("SELECT * FROM download_history WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadHistoryEntity>>

    @Query("SELECT * FROM download_history WHERE status = 'FAILED' ORDER BY createdAt DESC")
    fun getFailedDownloads(): Flow<List<DownloadHistoryEntity>>

    @Query("SELECT * FROM download_history WHERE status = 'COMPLETED' ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentCompleted(limit: Int): Flow<List<DownloadHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DownloadHistoryEntity)

    @Update
    suspend fun update(entity: DownloadHistoryEntity)

    @Query("UPDATE download_history SET downloadedBytes = :downloadedBytes, status = :status WHERE id = :id")
    suspend fun updateProgress(id: String, downloadedBytes: Long, status: String)

    @Query("UPDATE download_history SET status = :status, completedAt = :completedAt, contentUri = :contentUri, downloadedBytes = :fileSize WHERE id = :id")
    suspend fun markCompleted(id: String, status: String, completedAt: Long, contentUri: String?, fileSize: Long)

    @Query("UPDATE download_history SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String)

    @Query("UPDATE download_history SET status = 'PAUSED' WHERE status IN ('QUEUED', 'DOWNLOADING')")
    suspend fun pauseAllActive()

    @Query("UPDATE download_history SET status = 'CANCELLED' WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED')")
    suspend fun cancelAllActive()

    @Query("DELETE FROM download_history WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
