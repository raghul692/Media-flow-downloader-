package com.example.mediaflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.DownloadStatus
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val platform: String, // "YOUTUBE", "INSTAGRAM"
    val title: String,
    val thumbnail: String?,
    val mediaType: String, // "VIDEO", "AUDIO"
    val format: String, // "mp4", "m4a"
    val quality: String, // "1080p", "720p", "128 kbps"
    val fileName: String,
    val contentUri: String?,
    val fileSize: Long,
    val downloadedBytes: Long,
    val durationSeconds: Long,
    val status: String, // DownloadStatus enum name
    val createdAt: Long,
    val completedAt: Long?,
    val errorMessage: String?,
    val downloadUrl: String?
) {
    fun toDomainModel(): DownloadItem {
        val platformEnum = try {
            Platform.valueOf(platform)
        } catch (e: Exception) {
            Platform.YOUTUBE
        }
        val mediaTypeEnum = try {
            MediaType.valueOf(mediaType)
        } catch (e: Exception) {
            MediaType.VIDEO
        }
        val statusEnum = try {
            DownloadStatus.valueOf(status)
        } catch (e: Exception) {
            DownloadStatus.QUEUED
        }

        return DownloadItem(
            id = id,
            originalUrl = url,
            platform = platformEnum,
            title = title,
            thumbnail = thumbnail,
            mediaType = mediaTypeEnum,
            format = format,
            quality = quality,
            fileName = fileName,
            contentUri = contentUri,
            totalBytes = fileSize,
            downloadedBytes = downloadedBytes,
            durationSeconds = durationSeconds,
            status = statusEnum,
            errorMessage = errorMessage,
            createdAt = createdAt,
            completedAt = completedAt,
            downloadUrl = downloadUrl
        )
    }

    companion object {
        fun fromDomainModel(item: DownloadItem): DownloadHistoryEntity {
            return DownloadHistoryEntity(
                id = item.id,
                url = item.originalUrl,
                platform = item.platform.name,
                title = item.title,
                thumbnail = item.thumbnail,
                mediaType = item.mediaType.name,
                format = item.format,
                quality = item.quality,
                fileName = item.fileName,
                contentUri = item.contentUri,
                fileSize = item.totalBytes,
                downloadedBytes = item.downloadedBytes,
                durationSeconds = item.durationSeconds,
                status = item.status.name,
                createdAt = item.createdAt,
                completedAt = item.completedAt,
                errorMessage = item.errorMessage,
                downloadUrl = item.downloadUrl
            )
        }
    }
}
