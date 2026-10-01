package com.example.mediaflow.domain.usecase

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.MediaFormat
import com.example.mediaflow.domain.model.MediaMetadata
import com.example.mediaflow.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class DownloadUseCases(
    private val downloadRepository: DownloadRepository
) {
    fun getAllDownloads(): Flow<List<DownloadItem>> = downloadRepository.getAllDownloads()
    fun getActiveDownloads(): Flow<List<DownloadItem>> = downloadRepository.getActiveDownloads()
    fun getCompletedDownloads(): Flow<List<DownloadItem>> = downloadRepository.getCompletedDownloads()
    fun getFailedDownloads(): Flow<List<DownloadItem>> = downloadRepository.getFailedDownloads()

    suspend fun startDownload(metadata: MediaMetadata, format: MediaFormat): Result<String> {
        return downloadRepository.startDownload(metadata, format)
    }

    suspend fun pauseDownload(id: String): Result<Unit> = downloadRepository.pauseDownload(id)
    suspend fun resumeDownload(id: String): Result<Unit> = downloadRepository.resumeDownload(id)
    suspend fun cancelDownload(id: String): Result<Unit> = downloadRepository.cancelDownload(id)
    suspend fun retryDownload(id: String): Result<Unit> = downloadRepository.retryDownload(id)

    suspend fun pauseAll(): Result<Unit> = downloadRepository.pauseAll()
    suspend fun resumeAll(): Result<Unit> = downloadRepository.resumeAll()
    suspend fun cancelAll(): Result<Unit> = downloadRepository.cancelAll()
    suspend fun clearCompleted(): Result<Unit> = downloadRepository.clearCompleted()
    suspend fun retryAllFailed(): Result<Unit> = downloadRepository.retryAllFailed()
}
