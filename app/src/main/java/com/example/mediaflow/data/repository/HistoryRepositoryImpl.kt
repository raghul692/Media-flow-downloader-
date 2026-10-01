package com.example.mediaflow.data.repository

import com.example.mediaflow.data.database.dao.DownloadHistoryDao
import com.example.mediaflow.data.database.entity.DownloadHistoryEntity
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val dao: DownloadHistoryDao
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<DownloadItem>> {
        return dao.getAllHistory().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun searchAndFilterHistory(
        query: String,
        platform: Platform?,
        mediaType: MediaType?,
        sortNewestFirst: Boolean
    ): Flow<List<DownloadItem>> {
        return dao.getAllHistory().map { list ->
            list.map { it.toDomainModel() }
                .filter { item ->
                    val matchesQuery = if (query.isBlank()) true else {
                        item.title.contains(query, ignoreCase = true) || item.originalUrl.contains(query, ignoreCase = true)
                    }
                    val matchesPlatform = if (platform == null) true else {
                        item.platform == platform
                    }
                    val matchesMediaType = if (mediaType == null) true else {
                        item.mediaType == mediaType
                    }
                    matchesQuery && matchesPlatform && matchesMediaType
                }
                .let { filtered ->
                    if (sortNewestFirst) {
                        filtered.sortedByDescending { it.createdAt }
                    } else {
                        filtered.sortedBy { it.createdAt }
                    }
                }
        }
    }

    override suspend fun insertOrUpdate(item: DownloadItem) {
        dao.insertOrUpdate(DownloadHistoryEntity.fromDomainModel(item))
    }

    override suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }

    override suspend fun clearHistory() {
        dao.clearAll()
    }
}
