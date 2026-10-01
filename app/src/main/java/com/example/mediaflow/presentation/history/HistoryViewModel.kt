package com.example.mediaflow.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.usecase.DownloadUseCases
import com.example.mediaflow.domain.usecase.GetHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val searchQuery: String = "",
    val platformFilter: Platform? = null,
    val mediaTypeFilter: MediaType? = null,
    val sortNewestFirst: Boolean = true,
    val showClearDialog: Boolean = false
)

class HistoryViewModel(
    private val historyUseCase: GetHistoryUseCase,
    private val downloadUseCases: DownloadUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: StateFlow<List<DownloadItem>> = _uiState.flatMapLatest { state ->
        historyUseCase.searchAndFilter(
            query = state.searchQuery,
            platform = state.platformFilter,
            mediaType = state.mediaTypeFilter,
            sortNewestFirst = state.sortNewestFirst
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setPlatformFilter(platform: Platform?) {
        _uiState.value = _uiState.value.copy(platformFilter = platform)
    }

    fun setMediaTypeFilter(type: MediaType?) {
        _uiState.value = _uiState.value.copy(mediaTypeFilter = type)
    }

    fun toggleSort() {
        _uiState.value = _uiState.value.copy(sortNewestFirst = !_uiState.value.sortNewestFirst)
    }

    fun showClearConfirmDialog() {
        _uiState.value = _uiState.value.copy(showClearDialog = true)
    }

    fun dismissClearDialog() {
        _uiState.value = _uiState.value.copy(showClearDialog = false)
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyUseCase.clearAllHistory()
            dismissClearDialog()
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            historyUseCase.deleteHistoryItem(id)
        }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch {
            downloadUseCases.retryDownload(id)
        }
    }

    companion object {
        fun provideFactory(
            historyUseCase: GetHistoryUseCase,
            downloadUseCases: DownloadUseCases
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(historyUseCase, downloadUseCases) as T
            }
        }
    }
}
