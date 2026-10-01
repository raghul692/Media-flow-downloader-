package com.example.mediaflow.domain.model

data class DownloadItem(
    val id: String,
    val originalUrl: String,
    val platform: Platform,
    val title: String,
    val thumbnail: String? = null,
    val mediaType: MediaType,
    val format: String, // container e.g. "mp4", "m4a"
    val quality: String, // "1080p", "720p", "128 kbps"
    val fileName: String,
    val contentUri: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val downloadUrl: String? = null
) {
    val progressFloat: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercentage: Int
        get() = (progressFloat * 100).toInt()
}
