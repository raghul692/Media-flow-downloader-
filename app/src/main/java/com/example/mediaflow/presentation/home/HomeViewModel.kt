package com.example.mediaflow.presentation.home

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mediaflow.core.security.UrlSanitizer
import com.example.mediaflow.domain.model.AppSettings
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.repository.DownloadRepository
import com.example.mediaflow.domain.repository.SettingsRepository
import com.example.mediaflow.domain.usecase.ValidateUrlUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val urlInput: String = "",
    val errorMessage: String? = null,
    val detectedClipboardUrl: String? = null,
    val dismissedClipboardUrl: String? = null
)

class HomeViewModel(
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository,
    private val validateUrlUseCase: ValidateUrlUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val recentDownloads: StateFlow<List<DownloadItem>> = downloadRepository.getCompletedDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onUrlChanged(newUrl: String) {
        _uiState.value = _uiState.value.copy(urlInput = newUrl, errorMessage = null)
    }

    fun clearUrl() {
        _uiState.value = _uiState.value.copy(urlInput = "", errorMessage = null)
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).coerceToText(context).toString()
                onUrlChanged(text)
            }
        }
    }

    fun checkClipboardForSupportedUrl(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val description = clipboard.primaryClipDescription
                if (description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                    description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true
                ) {
                    val clipData = clipboard.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).coerceToText(context).toString().trim()
                        if (UrlSanitizer.isSupportedUrl(text) && text != _uiState.value.dismissedClipboardUrl) {
                            _uiState.value = _uiState.value.copy(detectedClipboardUrl = text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore clipboard access restrictions if backgrounded
        }
    }

    fun dismissClipboardBanner() {
        _uiState.value = _uiState.value.copy(
            dismissedClipboardUrl = _uiState.value.detectedClipboardUrl,
            detectedClipboardUrl = null
        )
    }

    fun acceptPolicy() {
        viewModelScope.launch {
            settingsRepository.setPolicyAgreed(true)
        }
    }

    fun validateUrlForNavigation(): String? {
        val input = _uiState.value.urlInput
        return when (val result = validateUrlUseCase(input)) {
            is ValidateUrlUseCase.ValidationResult.Valid -> {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                result.sanitizedUrl
            }
            is ValidateUrlUseCase.ValidationResult.Invalid -> {
                _uiState.value = _uiState.value.copy(errorMessage = result.reason)
                null
            }
        }
    }

    companion object {
        fun provideFactory(
            downloadRepository: DownloadRepository,
            settingsRepository: SettingsRepository,
            validateUrlUseCase: ValidateUrlUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(downloadRepository, settingsRepository, validateUrlUseCase) as T
            }
        }
    }
}
