package com.example.mediaflow.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mediaflow.domain.model.AppSettings
import com.example.mediaflow.domain.model.ThemeMode
import com.example.mediaflow.domain.repository.HistoryRepository
import com.example.mediaflow.domain.repository.SettingsRepository
import com.example.mediaflow.domain.repository.StorageRepository
import com.example.mediaflow.domain.repository.StorageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val storageStats: StorageStats = StorageStats(0L, 0L, 0L),
    val message: String? = null,
    val showPolicyDialog: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val storageRepository: StorageRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStorageStats()
    }

    fun loadStorageStats() {
        viewModelScope.launch {
            val stats = storageRepository.getStorageStats()
            _uiState.value = _uiState.value.copy(storageStats = stats)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnly(enabled)
        }
    }

    fun setDefaultVideoQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultVideoQuality(quality)
        }
    }

    fun setDefaultAudioQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultAudioQuality(quality)
        }
    }

    fun setMaxConcurrentDownloads(max: Int) {
        viewModelScope.launch {
            settingsRepository.setMaxConcurrentDownloads(max)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            storageRepository.clearTempFiles()
            loadStorageStats()
            _uiState.value = _uiState.value.copy(message = "Temporary cache cleared")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            _uiState.value = _uiState.value.copy(message = "Download history cleared")
        }
    }

    fun showPolicyNotice() {
        _uiState.value = _uiState.value.copy(showPolicyDialog = true)
    }

    fun dismissPolicyNotice() {
        _uiState.value = _uiState.value.copy(showPolicyDialog = false)
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            storageRepository: StorageRepository,
            historyRepository: HistoryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, storageRepository, historyRepository) as T
            }
        }
    }
}
