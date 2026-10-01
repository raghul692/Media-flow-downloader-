package com.example.mediaflow.data.storage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.example.mediaflow.core.common.Result
import com.example.mediaflow.core.security.FilenameSanitizer
import com.example.mediaflow.domain.model.MediaFileItem
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.repository.StorageRepository
import com.example.mediaflow.domain.repository.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreRepositoryImpl(
    private val context: Context
) : StorageRepository {

    override fun querySavedFiles(
        category: MediaType?,
        platform: Platform?,
        searchQuery: String
    ): Flow<List<MediaFileItem>> = flow {
        val files = mutableListOf<MediaFileItem>()

        if (category == null || category == MediaType.VIDEO) {
            files.addAll(queryVideoFiles())
        }

        if (category == null || category == MediaType.AUDIO) {
            files.addAll(queryAudioFiles())
        }

        val filtered = files.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.name.contains(searchQuery, ignoreCase = true)
            }
            val matchesPlatform = if (platform == null) true else {
                item.platform == platform
            }
            matchesSearch && matchesPlatform
        }.sortedByDescending { it.dateAddedSec }

        emit(filtered)
    }.flowOn(Dispatchers.IO)

    private fun queryVideoFiles(): List<MediaFileItem> {
        val items = mutableListOf<MediaFileItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.RELATIVE_PATH else MediaStore.Video.Media.DATA
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Video.Media.DATA} LIKE ?"
        }
        val selectionArgs = arrayOf("%MediaFlow%")

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "Video_$id"
                    val size = it.getLong(sizeCol)
                    val duration = it.getLong(durationCol)
                    val dateAdded = it.getLong(dateCol)
                    val mime = it.getString(mimeCol) ?: "video/mp4"

                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                    val detectedPlatform = when {
                        name.contains("YouTube", ignoreCase = true) || name.contains("yt_", ignoreCase = true) -> Platform.YOUTUBE
                        name.contains("Instagram", ignoreCase = true) || name.contains("ig_", ignoreCase = true) -> Platform.INSTAGRAM
                        else -> null
                    }

                    items.add(
                        MediaFileItem(
                            id = id,
                            contentUri = uri,
                            name = name,
                            mediaType = MediaType.VIDEO,
                            sizeBytes = size,
                            durationMs = duration,
                            dateAddedSec = dateAdded,
                            mimeType = mime,
                            platform = detectedPlatform
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Log/handle gracefully
        }
        return items
    }

    private fun queryAudioFiles(): List<MediaFileItem> {
        val items = mutableListOf<MediaFileItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Audio.Media.DATA} LIKE ?"
        }
        val selectionArgs = arrayOf("%MediaFlow%")

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "Audio_$id"
                    val size = it.getLong(sizeCol)
                    val duration = it.getLong(durationCol)
                    val dateAdded = it.getLong(dateCol)
                    val mime = it.getString(mimeCol) ?: "audio/mp4"

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

                    val detectedPlatform = when {
                        name.contains("YouTube", ignoreCase = true) || name.contains("yt_", ignoreCase = true) -> Platform.YOUTUBE
                        name.contains("Instagram", ignoreCase = true) || name.contains("ig_", ignoreCase = true) -> Platform.INSTAGRAM
                        else -> null
                    }

                    items.add(
                        MediaFileItem(
                            id = id,
                            contentUri = uri,
                            name = name,
                            mediaType = MediaType.AUDIO,
                            sizeBytes = size,
                            durationMs = duration,
                            dateAddedSec = dateAdded,
                            mimeType = mime,
                            platform = detectedPlatform
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Log/handle gracefully
        }
        return items
    }

    override suspend fun deleteFile(fileItem: MediaFileItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(fileItem.contentUri)
            val rows = context.contentResolver.delete(uri, null, null)
            Result.Success(rows > 0)
        } catch (e: Exception) {
            Result.Error(e, "Could not delete file: ${e.localizedMessage}")
        }
    }

    override suspend fun renameFile(fileItem: MediaFileItem, newName: String): Result<MediaFileItem> = withContext(Dispatchers.IO) {
        try {
            val ext = fileItem.name.substringAfterLast('.', "")
            val safeName = FilenameSanitizer.sanitize(newName, ext)
            val uri = Uri.parse(fileItem.contentUri)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
            }

            val updated = context.contentResolver.update(uri, values, null, null)
            if (updated > 0) {
                Result.Success(fileItem.copy(name = safeName))
            } else {
                Result.Error(IllegalStateException("No rows updated"), "Failed to rename file")
            }
        } catch (e: Exception) {
            Result.Error(e, "Rename failed: ${e.localizedMessage}")
        }
    }

    override suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = totalBytes - freeBytes

            // Calculate size of MediaFlow downloaded files
            var mediaFlowBytes = 0L
            val videoFiles = queryVideoFiles()
            val audioFiles = queryAudioFiles()
            for (f in videoFiles) mediaFlowBytes += f.sizeBytes
            for (f in audioFiles) mediaFlowBytes += f.sizeBytes

            StorageStats(
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                mediaFlowBytes = mediaFlowBytes
            )
        } catch (e: Exception) {
            StorageStats(0L, 0L, 0L)
        }
    }

    override suspend fun clearTempFiles(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var clearedBytes = 0L
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    clearedBytes += file.length()
                    file.delete()
                }
            }
            Result.Success(clearedBytes)
        } catch (e: Exception) {
            Result.Error(e, "Failed to clear temp cache: ${e.localizedMessage}")
        }
    }
}
