package com.example.mangareader.ui.detail

import android.app.Application // For DownloadManager context
import androidx.lifecycle.*
import com.example.mangareader.data.MangaRepository
import com.example.mangareader.data.model.ChapterInfo
import com.example.mangareader.data.model.MangaInfo
import com.example.mangareader.download.DownloadManager
import com.example.mangareader.ui.browse.Event // Reusing Event class
import kotlinx.coroutines.launch

class MangaDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MangaRepository() // Inject in real app
    private val downloadManager = DownloadManager(application) // Inject in real app

    private val _mangaDetails = MutableLiveData<MangaInfo?>()
    val mangaDetails: LiveData<MangaInfo?> = _mangaDetails

    private val _chapters = MutableLiveData<List<ChapterInfo>>()
    val chapters: LiveData<List<ChapterInfo>> = _chapters

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _navigateToReader = MutableLiveData<Event<List<String>>>()
    val navigateToReader: LiveData<Event<List<String>>> = _navigateToReader


    fun loadMangaData(mangaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Fetch Manga Details
            val detailsResult = repository.getMangaDetails(mangaId)
            detailsResult.onSuccess {
                _mangaDetails.value = it
            }.onFailure {
                _errorMessage.value = "Error loading details: ${it.message}"
                _isLoading.value = false // Stop loading if details fail
                return@launch
            }

            // Fetch Chapters
            // Assuming English for now, could be a preference later
            val chaptersResult = repository.getMangaChapters(mangaId, languages = listOf("en"))
            chaptersResult.onSuccess {
                _chapters.value = it.sortedByDescending { chapter -> chapter.chapterNumber?.toFloatOrNull() ?: 0f } // Show latest first
            }.onFailure {
                _errorMessage.value = "Error loading chapters: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun onChapterClicked(chapterInfo: ChapterInfo) {
        viewModelScope.launch {
            _isLoading.value = true // Use main loading indicator for page fetching
            val pagesResult = repository.getChapterPageImageUrls(chapterInfo.id)
            pagesResult.onSuccess { pageUrls ->
                if (pageUrls.isNotEmpty()) {
                    _navigateToReader.value = Event(pageUrls)
                } else {
                    _errorMessage.value = "No pages found for this chapter."
                }
            }.onFailure { error ->
                _errorMessage.value = "Failed to load pages: ${error.message}"
            }
            _isLoading.value = false
        }
    }

    fun downloadChapter(chapterInfo: ChapterInfo) {
        val manga = _mangaDetails.value
        if (manga == null) {
            _errorMessage.value = "Manga details not loaded, cannot download chapter."
            return
        }
        viewModelScope.launch {
            downloadManager.enqueueChapterDownload(manga, chapterInfo)
            // Optionally show a toast or some feedback
            // This could also update a LiveData that the Fragment observes to show "Added to queue"
            _errorMessage.value = "'${chapterInfo.title ?: "Chapter "+chapterInfo.chapterNumber}' added to download queue." // Corrected feedback string
        }
    }
    fun onErrorMessageShown() {
        _errorMessage.value = null
    }
}
