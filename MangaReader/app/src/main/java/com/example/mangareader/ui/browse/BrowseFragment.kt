package com.example.mangareader.ui.browse

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangareader.R
import com.example.mangareader.ReaderActivity // Import ReaderActivity
// Removed AlertDialog and ChapterInfo imports as they are no longer used here
import com.example.mangareader.MainActivity // For navigation

class BrowseFragment : Fragment() {

    private val browseViewModel: BrowseViewModel by viewModels()
    private lateinit var mangaListAdapter: MangaListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar // Main progress bar for list
    private lateinit var progressBarChapter: ProgressBar // Separate progress bar for chapter loading
    private lateinit var errorTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_browse, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_manga_list)
        progressBar = view.findViewById(R.id.progress_bar_loading)
        // Ensure you add progress_bar_chapter_loading to fragment_browse.xml
        // It can be similar to progress_bar_loading, perhaps smaller or in a different position
        progressBarChapter = view.findViewById(R.id.progress_bar_chapter_loading)
        errorTextView = view.findViewById(R.id.text_view_error_message)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        browseViewModel.mangaList.observe(viewLifecycleOwner) { manga ->
            mangaListAdapter.updateData(manga)
            errorTextView.isVisible = manga.isEmpty() && browseViewModel.errorMessage.value != null
            recyclerView.isVisible = manga.isNotEmpty()
        }

        browseViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
            if (isLoading) {
                errorTextView.isVisible = false
            }
        }

        browseViewModel.isLoadingChapter.observe(viewLifecycleOwner) { isLoading ->
            // Show a specific loading indicator when fetching chapter details,
            // e.g., a small indeterminate progress bar overlay or disable UI elements
            progressBarChapter.isVisible = isLoading
            // Could also implement a more noticeable overlay
        }

        browseViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                val currentList = browseViewModel.mangaList.value
                // Show error in TextView if the main list is empty and not currently loading the main list
                if (currentList.isNullOrEmpty() && browseViewModel.isLoading.value == false) {
                     errorTextView.text = errorMessage
                     errorTextView.isVisible = true
                }
                // Show as Toast if it's a chapter-related error (isLoadingChapter was true or just finished)
                // or if the main list is already populated (so error is likely related to a click action)
                else if (browseViewModel.isLoadingChapter.value == false || !currentList.isNullOrEmpty()) {
                     Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                browseViewModel.onErrorMessageShown()
            } else {
                errorTextView.isVisible = false
            }
        }

        browseViewModel.navigateToReader.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { pageUrls ->
                if (context != null) {
                    val intent = ReaderActivity.newIntent(requireContext(), ArrayList(pageUrls))
                    startActivity(intent)
                }
            }
        }

        if (browseViewModel.mangaList.value.isNullOrEmpty()) {
             browseViewModel.fetchPopularManga()
        }
    }

    private fun setupRecyclerView() {
        mangaListAdapter = MangaListAdapter(emptyList()) { mangaInfo ->
            (activity as? MainActivity)?.navigateToMangaDetail(mangaInfo.id)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mangaListAdapter
        }
    }

    companion object {
        fun newInstance() = BrowseFragment()
    }
}
