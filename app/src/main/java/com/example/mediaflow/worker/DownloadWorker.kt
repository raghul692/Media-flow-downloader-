package com.example.mediaflow.worker

import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mediaflow.data.database.MediaFlowDatabase
import com.example.mediaflow.domain.model.DownloadStatus
import com.example.mediaflow.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_DOWNLOAD_ID = "key_download_id"
        const val KEY_DOWNLOAD_URL = "key_download_url"
        const val KEY_TITLE = "key_title"
        const val KEY_MEDIA_TYPE = "key_media_type"
        const val KEY_CONTAINER = "key_container"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_TOTAL_BYTES = "key_total_bytes"

        const val KEY_PROGRESS_PERCENT = "key_progress_percent"
        const val KEY_DOWNLOADED_BYTES = "key_downloaded_bytes"
        const val KEY_SPEED_BYTES = "key_speed_bytes"
        const val KEY_ETA_SECONDS = "key_eta_seconds"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val database = MediaFlowDatabase.getInstance(context)
    private val dao = database.downloadHistoryDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Media Item"
        val mediaTypeStr = inputData.getString(KEY_MEDIA_TYPE) ?: MediaType.VIDEO.name
        val container = inputData.getString(KEY_CONTAINER) ?: "mp4"
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "${title.take(30)}.$container"
        val estimatedTotal = inputData.getLong(KEY_TOTAL_BYTES, 0L)

        val notificationId = downloadId.hashCode()

        // Initialize Foreground notification
        DownloadNotificationHelper.createNotificationChannel(context)
        val initialNotification = DownloadNotificationHelper.buildProgressNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            progressPercent = 0,
            downloadedBytes = 0L,
            totalBytes = estimatedTotal,
            speedBytesPerSec = 0L,
            etaSeconds = 0L
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setForeground(ForegroundInfo(notificationId, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            } else {
                setForeground(ForegroundInfo(notificationId, initialNotification))
            }
        } catch (e: Exception) {
            // In case foreground cannot be posted immediately
        }

        dao.updateProgress(downloadId, 0L, DownloadStatus.DOWNLOADING.name)

        val tempFile = File(context.cacheDir, "temp_$downloadId.$container")
        var downloadedBytes = 0L

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) MediaFlow/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = "HTTP error ${response.code}: ${response.message}"
                dao.markFailed(downloadId, errorMsg)
                DownloadNotificationHelper.showFailureNotification(context, notificationId, title, errorMsg)
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                dao.markFailed(downloadId, "Empty response body from media provider")
                DownloadNotificationHelper.showFailureNotification(context, notificationId, title, "Empty response")
                return@withContext Result.failure()
            }

            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) contentLength else estimatedTotal

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L
            var currentSpeed = 0L
            var currentEta = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            tempFile.delete()
                            dao.updateProgress(downloadId, downloadedBytes, DownloadStatus.CANCELLED.name)
                            return@withContext Result.failure()
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        bytesSinceLastUpdate += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (elapsed >= 500) {
                            currentSpeed = (bytesSinceLastUpdate * 1000) / elapsed
                            currentEta = if (currentSpeed > 0 && totalBytes > downloadedBytes) {
                                (totalBytes - downloadedBytes) / currentSpeed
                            } else 0L

                            val progressPercent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else 0

                            setProgress(
                                workDataOf(
                                    KEY_PROGRESS_PERCENT to progressPercent,
                                    KEY_DOWNLOADED_BYTES to downloadedBytes,
                                    KEY_SPEED_BYTES to currentSpeed,
                                    KEY_ETA_SECONDS to currentEta
                                )
                            )

                            dao.updateProgress(downloadId, downloadedBytes, DownloadStatus.DOWNLOADING.name)

                            // Update foreground notification
                            val progressNotification = DownloadNotificationHelper.buildProgressNotification(
                                context = context,
                                notificationId = notificationId,
                                title = title,
                                progressPercent = progressPercent,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                speedBytesPerSec = currentSpeed,
                                etaSeconds = currentEta
                            )
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    setForeground(ForegroundInfo(notificationId, progressNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
                                } else {
                                    setForeground(ForegroundInfo(notificationId, progressNotification))
                                }
                            } catch (e: Exception) {
                                // Notification rate limiter safety
                            }

                            lastUpdateTime = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                }
            }

            // PROCESSING Phase: Save to Android MediaStore
            dao.updateProgress(downloadId, downloadedBytes, DownloadStatus.PROCESSING.name)
            val isVideo = mediaTypeStr == MediaType.VIDEO.name
            val savedUri = saveToMediaStore(
                context = context,
                sourceFile = tempFile,
                fileName = fileName,
                isVideo = isVideo,
                container = container
            )

            tempFile.delete()

            if (savedUri != null) {
                val finalSize = downloadedBytes
                dao.markCompleted(
                    id = downloadId,
                    status = DownloadStatus.COMPLETED.name,
                    completedAt = System.currentTimeMillis(),
                    contentUri = savedUri.toString(),
                    fileSize = finalSize
                )
                DownloadNotificationHelper.showCompletionNotification(
                    context = context,
                    notificationId = notificationId,
                    title = title,
                    contentUri = savedUri.toString()
                )
                Result.success()
            } else {
                dao.markFailed(downloadId, "Unable to save media into MediaStore storage")
                DownloadNotificationHelper.showFailureNotification(context, notificationId, title, "Storage write error")
                Result.failure()
            }
        } catch (e: Exception) {
            tempFile.delete()
            val errorMsg = e.localizedMessage ?: "Unknown download error"
            dao.markFailed(downloadId, errorMsg)
            DownloadNotificationHelper.showFailureNotification(context, notificationId, title, errorMsg)
            Result.failure()
        }
    }

    private fun saveToMediaStore(
        context: Context,
        sourceFile: File,
        fileName: String,
        isVideo: Boolean,
        container: String
    ): Uri? {
        val resolver = context.contentResolver
        val mimeType = if (isVideo) "video/$container" else "audio/$container"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val subDir = if (isVideo) "Movies/MediaFlow" else "Music/MediaFlow"
                put(MediaStore.MediaColumns.RELATIVE_PATH, subDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collectionUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val insertedUri = resolver.insert(collectionUri, values) ?: return null

        try {
            val outStream: OutputStream? = resolver.openOutputStream(insertedUri)
            sourceFile.inputStream().use { input ->
                outStream?.use { output ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(insertedUri, values, null, null)
            }

            return insertedUri
        } catch (e: Exception) {
            resolver.delete(insertedUri, null, null)
            return null
        }
    }
}
