package com.example.mediaflow.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mediaflow.domain.model.MediaFileItem
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.usecase.LibraryUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryCategory(val displayName: String) {
    ALL("All"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    YOUTUBE("YouTube"),
    INSTAGRAM("Instagram")
}

data class LibraryUiState(
    val searchQuery: String = "",
    val selectedCategory: LibraryCategory = LibraryCategory.ALL,
    val isGridView: Boolean = false,
    val fileToRename: MediaFileItem? = null,
    val fileDetails: MediaFileItem? = null,
    val message: String? = null
)

class LibraryViewModel(
    private val libraryUseCases: LibraryUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val files: StateFlow<List<MediaFileItem>> = _uiState.flatMapLatest { state ->
        val category = when (state.selectedCategory) {
            LibraryCategory.VIDEOS -> MediaType.VIDEO
            LibraryCategory.AUDIO -> MediaType.AUDIO
            else -> null
        }
        val platform = when (state.selectedCategory) {
            LibraryCategory.YOUTUBE -> Platform.YOUTUBE
            LibraryCategory.INSTAGRAM -> Platform.INSTAGRAM
            else -> null
        }
        libraryUseCases.getSavedFiles(category, platform, state.searchQuery)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectCategory(category: LibraryCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun toggleGridView() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun promptRename(file: MediaFileItem) {
        _uiState.value = _uiState.value.copy(fileToRename = file)
    }

    fun dismissRename() {
        _uiState.value = _uiState.value.copy(fileToRename = null)
    }

    fun renameFile(newName: String) {
        val file = _uiState.value.fileToRename ?: return
        viewModelScope.launch {
            libraryUseCases.renameFile(file, newName)
            dismissRename()
        }
    }

    fun showFileDetails(file: MediaFileItem) {
        _uiState.value = _uiState.value.copy(fileDetails = file)
    }

    fun dismissFileDetails() {
        _uiState.value = _uiState.value.copy(fileDetails = null)
    }

    fun deleteFile(file: MediaFileItem) {
        viewModelScope.launch {
            libraryUseCases.deleteFile(file)
        }
    }

    companion object {
        fun provideFactory(
            libraryUseCases: LibraryUseCases
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(libraryUseCases) as T
            }
        }
    }
}
