package com.example.mediaflow.domain.repository

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.domain.model.MediaFileItem
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun querySavedFiles(
        category: MediaType? = null,
        platform: Platform? = null,
        searchQuery: String = ""
    ): Flow<List<MediaFileItem>>

    suspend fun deleteFile(fileItem: MediaFileItem): Result<Boolean>
    suspend fun renameFile(fileItem: MediaFileItem, newName: String): Result<MediaFileItem>
    suspend fun getStorageStats(): StorageStats
    suspend fun clearTempFiles(): Result<Long>
}

data class StorageStats(
    val usedBytes: Long,
    val freeBytes: Long,
    val mediaFlowBytes: Long
)
