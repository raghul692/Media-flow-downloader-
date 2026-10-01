package com.example.mediaflow.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.usecase.DownloadUseCases
import com.example.mediaflow.domain.usecase.GetHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadsTab(val title: String) {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

class DownloadsViewModel(
    private val downloadUseCases: DownloadUseCases,
    private val historyUseCase: GetHistoryUseCase
) : ViewModel() {

    private val _currentTab = MutableStateFlow(DownloadsTab.ACTIVE)
    val currentTab: StateFlow<DownloadsTab> = _currentTab.asStateFlow()

    val activeDownloads: StateFlow<List<DownloadItem>> = downloadUseCases.getActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads: StateFlow<List<DownloadItem>> = downloadUseCases.getCompletedDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val failedDownloads: StateFlow<List<DownloadItem>> = downloadUseCases.getFailedDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: DownloadsTab) {
        _currentTab.value = tab
    }

    fun pauseDownload(id: String) {
        viewModelScope.launch {
            downloadUseCases.pauseDownload(id)
        }
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch {
            downloadUseCases.resumeDownload(id)
        }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            downloadUseCases.cancelDownload(id)
        }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch {
            downloadUseCases.retryDownload(id)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            historyUseCase.deleteHistoryItem(id)
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            downloadUseCases.pauseAll()
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            downloadUseCases.resumeAll()
        }
    }

    fun cancelAll() {
        viewModelScope.launch {
            downloadUseCases.cancelAll()
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadUseCases.clearCompleted()
        }
    }

    fun retryAllFailed() {
        viewModelScope.launch {
            downloadUseCases.retryAllFailed()
        }
    }

    companion object {
        fun provideFactory(
            downloadUseCases: DownloadUseCases,
            historyUseCase: GetHistoryUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DownloadsViewModel(downloadUseCases, historyUseCase) as T
            }
        }
    }
}
