package com.example.mediaflow.domain.repository

import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<DownloadItem>>
    fun searchAndFilterHistory(
        query: String = "",
        platform: Platform? = null,
        mediaType: MediaType? = null,
        sortNewestFirst: Boolean = true
    ): Flow<List<DownloadItem>>

    suspend fun insertOrUpdate(item: DownloadItem)
    suspend fun deleteById(id: String)
    suspend fun clearHistory()
}
