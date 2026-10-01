package com.example.mediaflow.domain.usecase

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.domain.model.MediaFileItem
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.repository.StorageRepository
import com.example.mediaflow.domain.repository.StorageStats
import kotlinx.coroutines.flow.Flow

class LibraryUseCases(
    private val storageRepository: StorageRepository
) {
    fun getSavedFiles(
        category: MediaType? = null,
        platform: Platform? = null,
        searchQuery: String = ""
    ): Flow<List<MediaFileItem>> = storageRepository.querySavedFiles(category, platform, searchQuery)

    suspend fun deleteFile(item: MediaFileItem): Result<Boolean> = storageRepository.deleteFile(item)

    suspend fun renameFile(item: MediaFileItem, newName: String): Result<MediaFileItem> =
        storageRepository.renameFile(item, newName)

    suspend fun getStorageStats(): StorageStats = storageRepository.getStorageStats()

    suspend fun clearTempFiles(): Result<Long> = storageRepository.clearTempFiles()
}
