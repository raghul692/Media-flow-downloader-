package com.example.mediaflow.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mediaflow.core.common.Result
import com.example.mediaflow.core.security.FilenameSanitizer
import com.example.mediaflow.data.database.dao.DownloadHistoryDao
import com.example.mediaflow.data.database.entity.DownloadHistoryEntity
import com.example.mediaflow.data.preferences.SettingsPreferencesDataSource
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.DownloadStatus
import com.example.mediaflow.domain.model.MediaFormat
import com.example.mediaflow.domain.model.MediaMetadata
import com.example.mediaflow.domain.repository.DownloadRepository
import com.example.mediaflow.worker.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class DownloadRepositoryImpl(
    private val context: Context,
    private val dao: DownloadHistoryDao,
    private val preferencesDataSource: SettingsPreferencesDataSource
) : DownloadRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun getAllDownloads(): Flow<List<DownloadItem>> {
        return combine(
            dao.getAllHistory(),
            workManager.getWorkInfosByTagFlow("mediaflow_download")
        ) { historyList, workInfos ->
            val workMap = workInfos.associateBy { it.tags.firstOrNull { tag -> tag.startsWith("id_") }?.removePrefix("id_") }

            historyList.map { entity ->
                val base = entity.toDomainModel()
                val workInfo = workMap[base.id]
                if (workInfo != null && !base.status.isFinished) {
                    val progress = workInfo.progress
                    val downloaded = progress.getLong(DownloadWorker.KEY_DOWNLOADED_BYTES, base.downloadedBytes)
                    val speed = progress.getLong(DownloadWorker.KEY_SPEED_BYTES, 0L)
                    val eta = progress.getLong(DownloadWorker.KEY_ETA_SECONDS, 0L)

                    val status = when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> DownloadStatus.QUEUED
                        WorkInfo.State.RUNNING -> DownloadStatus.DOWNLOADING
                        WorkInfo.State.SUCCEEDED -> DownloadStatus.COMPLETED
                        WorkInfo.State.FAILED -> DownloadStatus.FAILED
                        WorkInfo.State.CANCELLED -> DownloadStatus.CANCELLED
                        WorkInfo.State.BLOCKED -> DownloadStatus.QUEUED
                    }

                    base.copy(
                        downloadedBytes = downloaded,
                        speedBytesPerSec = speed,
                        etaSeconds = eta,
                        status = status
                    )
                } else {
                    base
                }
            }
        }
    }

    override fun getActiveDownloads(): Flow<List<DownloadItem>> {
        return getAllDownloads().map { list ->
            list.filter { it.status.isActive }
        }
    }

    override fun getCompletedDownloads(): Flow<List<DownloadItem>> {
        return dao.getCompletedDownloads().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getFailedDownloads(): Flow<List<DownloadItem>> {
        return dao.getFailedDownloads().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun getDownloadById(id: String): DownloadItem? {
        return dao.getById(id)?.toDomainModel()
    }

    override suspend fun startDownload(
        metadata: MediaMetadata,
        selectedFormat: MediaFormat
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = selectedFormat.downloadUrl
                ?: return@withContext Result.Error(
                    IllegalArgumentException("No download URL provided for format"),
                    "Could not initiate download: Stream URL unavailable."
                )

            val downloadId = UUID.randomUUID().toString()
            val safeFileName = FilenameSanitizer.sanitize(metadata.title, selectedFormat.container)

            val historyEntity = DownloadHistoryEntity(
                id = downloadId,
                url = metadata.sanitizedUrl,
                platform = metadata.platform.name,
                title = metadata.title,
                thumbnail = metadata.thumbnail,
                mediaType = selectedFormat.mediaType.name,
                format = selectedFormat.container,
                quality = selectedFormat.qualityLabel,
                fileName = safeFileName,
                contentUri = null,
                fileSize = selectedFormat.estimatedSizeBytes,
                downloadedBytes = 0L,
                durationSeconds = metadata.durationSeconds,
                status = DownloadStatus.QUEUED.name,
                createdAt = System.currentTimeMillis(),
                completedAt = null,
                errorMessage = null,
                downloadUrl = downloadUrl
            )

            dao.insertOrUpdate(historyEntity)

            val settings = preferencesDataSource.settingsFlow.first()
            val networkType = if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .addTag("mediaflow_download")
                .addTag("id_$downloadId")
                .setInputData(
                    workDataOf(
                        DownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                        DownloadWorker.KEY_DOWNLOAD_URL to downloadUrl,
                        DownloadWorker.KEY_TITLE to metadata.title,
                        DownloadWorker.KEY_MEDIA_TYPE to selectedFormat.mediaType.name,
                        DownloadWorker.KEY_CONTAINER to selectedFormat.container,
                        DownloadWorker.KEY_FILE_NAME to safeFileName,
                        DownloadWorker.KEY_TOTAL_BYTES to selectedFormat.estimatedSizeBytes
                    )
                )
                .build()

            workManager.enqueueUniqueWork(
                "download_$downloadId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Result.Success(downloadId)
        } catch (e: Exception) {
            Result.Error(e, "Could not start download: ${e.localizedMessage}")
        }
    }

    override suspend fun pauseDownload(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workManager.cancelUniqueWork("download_$id")
            val item = dao.getById(id)
            if (item != null) {
                dao.update(item.copy(status = DownloadStatus.PAUSED.name))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Pause failed")
        }
    }

    override suspend fun resumeDownload(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = dao.getById(id) ?: return@withContext Result.Error(IllegalArgumentException("Item not found"))
            val downloadUrl = item.downloadUrl ?: return@withContext Result.Error(IllegalArgumentException("Missing URL"))

            val settings = preferencesDataSource.settingsFlow.first()
            val networkType = if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .addTag("mediaflow_download")
                .addTag("id_$id")
                .setInputData(
                    workDataOf(
                        DownloadWorker.KEY_DOWNLOAD_ID to id,
                        DownloadWorker.KEY_DOWNLOAD_URL to downloadUrl,
                        DownloadWorker.KEY_TITLE to item.title,
                        DownloadWorker.KEY_MEDIA_TYPE to item.mediaType,
                        DownloadWorker.KEY_CONTAINER to item.format,
                        DownloadWorker.KEY_FILE_NAME to item.fileName,
                        DownloadWorker.KEY_TOTAL_BYTES to item.fileSize
                    )
                )
                .build()

            dao.update(item.copy(status = DownloadStatus.QUEUED.name))
            workManager.enqueueUniqueWork(
                "download_$id",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Resume failed")
        }
    }

    override suspend fun cancelDownload(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workManager.cancelUniqueWork("download_$id")
            val item = dao.getById(id)
            if (item != null) {
                dao.update(item.copy(status = DownloadStatus.CANCELLED.name))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Cancel failed")
        }
    }

    override suspend fun retryDownload(id: String): Result<Unit> {
        return resumeDownload(id)
    }

    override suspend fun pauseAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workManager.cancelAllWorkByTag("mediaflow_download")
            dao.pauseAllActive()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to pause all")
        }
    }

    override suspend fun resumeAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val paused = dao.getAllHistory().first().filter { it.status == DownloadStatus.PAUSED.name }
            for (p in paused) {
                resumeDownload(p.id)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to resume all")
        }
    }

    override suspend fun cancelAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workManager.cancelAllWorkByTag("mediaflow_download")
            dao.cancelAllActive()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to cancel all")
        }
    }

    override suspend fun clearCompleted(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.clearCompleted()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to clear completed")
        }
    }

    override suspend fun retryAllFailed(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val failed = dao.getFailedDownloads().first()
            for (f in failed) {
                resumeDownload(f.id)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to retry all")
        }
    }
}
