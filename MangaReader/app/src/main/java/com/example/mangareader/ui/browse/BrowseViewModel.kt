package com.example.mangareader.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangareader.data.MangaRepository
import com.example.mangareader.data.model.MangaInfo
import com.example.mangareader.data.model.ChapterInfo // Import ChapterInfo
import kotlinx.coroutines.launch

class BrowseViewModel : ViewModel() {
    private val repository = MangaRepository()

    private val _mangaList = MutableLiveData<List<MangaInfo>>()
    val mangaList: LiveData<List<MangaInfo>> = _mangaList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Removed: _isLoadingChapter, _navigateToReader, _selectedMangaInfo, _selectedMangaChapters

    fun fetchPopularManga() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.getPopularMangaList()
                result.onSuccess { mangas ->
                    _mangaList.value = mangas
                }.onFailure { error ->
                    _mangaList.value = emptyList()
                    _errorMessage.value = "Failed to load manga: ${error.message}"
                }
            } catch (e: Exception) {
                _mangaList.value = emptyList()
                _errorMessage.value = "An unexpected error occurred: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Removed onMangaClicked, downloadChapter, clearSelectedMangaChapters methods

    fun refreshPopularManga() {
        fetchPopularManga()
    }

    fun onErrorMessageShown() {
        _errorMessage.value = null
    }
}

// Helper class for one-time navigation/event LiveData
open class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
    fun peekContent(): T = content // Use to view content without marking as handled
}
