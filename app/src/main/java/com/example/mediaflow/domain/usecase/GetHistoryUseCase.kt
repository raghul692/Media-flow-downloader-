package com.example.mediaflow.domain.usecase

import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    fun getAllHistory(): Flow<List<DownloadItem>> = historyRepository.getAllHistory()

    fun searchAndFilter(
        query: String = "",
        platform: Platform? = null,
        mediaType: MediaType? = null,
        sortNewestFirst: Boolean = true
    ): Flow<List<DownloadItem>> = historyRepository.searchAndFilterHistory(query, platform, mediaType, sortNewestFirst)

    suspend fun deleteHistoryItem(id: String) = historyRepository.deleteById(id)

    suspend fun clearAllHistory() = historyRepository.clearHistory()
}
