package com.example.mediaflow.domain.repository

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.MediaFormat
import com.example.mediaflow.domain.model.MediaMetadata
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadItem>>
    fun getActiveDownloads(): Flow<List<DownloadItem>>
    fun getCompletedDownloads(): Flow<List<DownloadItem>>
    fun getFailedDownloads(): Flow<List<DownloadItem>>
    suspend fun getDownloadById(id: String): DownloadItem?

    suspend fun startDownload(metadata: MediaMetadata, selectedFormat: MediaFormat): Result<String>
    suspend fun pauseDownload(id: String): Result<Unit>
    suspend fun resumeDownload(id: String): Result<Unit>
    suspend fun cancelDownload(id: String): Result<Unit>
    suspend fun retryDownload(id: String): Result<Unit>

    suspend fun pauseAll(): Result<Unit>
    suspend fun resumeAll(): Result<Unit>
    suspend fun cancelAll(): Result<Unit>
    suspend fun clearCompleted(): Result<Unit>
    suspend fun retryAllFailed(): Result<Unit>
}
