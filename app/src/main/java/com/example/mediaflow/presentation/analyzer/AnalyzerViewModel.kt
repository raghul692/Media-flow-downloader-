package com.example.mediaflow.presentation.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mediaflow.core.common.Result
import com.example.mediaflow.domain.model.MediaFormat
import com.example.mediaflow.domain.model.MediaMetadata
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.usecase.AnalyzeMediaUseCase
import com.example.mediaflow.domain.usecase.DownloadUseCases
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AnalyzerUiState {
    data object Idle : AnalyzerUiState()
    data class Loading(val url: String) : AnalyzerUiState()
    data class Success(
        val metadata: MediaMetadata,
        val selectedFormat: MediaFormat,
        val activeTab: MediaType = MediaType.VIDEO
    ) : AnalyzerUiState()
    data class Error(val message: String, val url: String) : AnalyzerUiState()
}

class AnalyzerViewModel(
    private val analyzeMediaUseCase: AnalyzeMediaUseCase,
    private val downloadUseCases: DownloadUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyzerUiState>(AnalyzerUiState.Idle)
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents: SharedFlow<String> = _navigationEvents.asSharedFlow()

    fun analyzeUrl(url: String) {
        _uiState.value = AnalyzerUiState.Loading(url)
        viewModelScope.launch {
            when (val result = analyzeMediaUseCase(url)) {
                is Result.Success -> {
                    val metadata = result.data
                    val defaultFormat = metadata.formats.firstOrNull { it.mediaType == MediaType.VIDEO }
                        ?: metadata.formats.firstOrNull()

                    if (defaultFormat != null) {
                        _uiState.value = AnalyzerUiState.Success(
                            metadata = metadata,
                            selectedFormat = defaultFormat,
                            activeTab = defaultFormat.mediaType
                        )
                    } else {
                        _uiState.value = AnalyzerUiState.Error(
                            message = "No downloadable formats are permitted or available for this media.",
                            url = url
                        )
                    }
                }
                is Result.Error -> {
                    val userMsg = result.userMessage ?: result.exception.localizedMessage ?: "Failed to process media"
                    _uiState.value = AnalyzerUiState.Error(message = userMsg, url = url)
                }
                is Result.Loading -> {
                    _uiState.value = AnalyzerUiState.Loading(url)
                }
            }
        }
    }

    fun selectTab(tab: MediaType) {
        val current = _uiState.value as? AnalyzerUiState.Success ?: return
        val matchingFormat = current.metadata.formats.firstOrNull { it.mediaType == tab }
            ?: current.selectedFormat

        _uiState.value = current.copy(
            activeTab = tab,
            selectedFormat = matchingFormat
        )
    }

    fun selectFormat(format: MediaFormat) {
        val current = _uiState.value as? AnalyzerUiState.Success ?: return
        _uiState.value = current.copy(selectedFormat = format)
    }

    fun startDownload() {
        val current = _uiState.value as? AnalyzerUiState.Success ?: return
        viewModelScope.launch {
            val result = downloadUseCases.startDownload(current.metadata, current.selectedFormat)
            if (result is Result.Success) {
                _navigationEvents.emit(result.data)
            }
        }
    }

    companion object {
        fun provideFactory(
            analyzeMediaUseCase: AnalyzeMediaUseCase,
            downloadUseCases: DownloadUseCases
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AnalyzerViewModel(analyzeMediaUseCase, downloadUseCases) as T
            }
        }
    }
}
