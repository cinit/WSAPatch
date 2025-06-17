package com.example.mangareader.ui.detail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mangareader.R
import com.example.mangareader.ReaderActivity
import com.example.mangareader.data.model.MangaInfo
import com.example.mangareader.MainActivity // Import MainActivity for navigation

class MangaDetailFragment : Fragment() {

    private val viewModel: MangaDetailViewModel by viewModels()
    private lateinit var chapterListAdapter: ChapterListAdapter

    private lateinit var coverImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var authorTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var chaptersRecyclerView: RecyclerView
    private lateinit var loadingProgressBar: ProgressBar


    companion object {
        private const val ARG_MANGA_ID = "manga_id"
        fun newInstance(mangaId: String): MangaDetailFragment {
            val fragment = MangaDetailFragment()
            val args = Bundle()
            args.putString(ARG_MANGA_ID, mangaId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manga_detail, container, false)
        coverImageView = view.findViewById(R.id.image_view_detail_cover)
        titleTextView = view.findViewById(R.id.text_view_detail_title)
        authorTextView = view.findViewById(R.id.text_view_detail_author)
        statusTextView = view.findViewById(R.id.text_view_detail_status)
        descriptionTextView = view.findViewById(R.id.text_view_detail_description)
        chaptersRecyclerView = view.findViewById(R.id.recycler_view_chapters)
        loadingProgressBar = view.findViewById(R.id.progress_bar_detail_loading)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        val mangaId = arguments?.getString(ARG_MANGA_ID)
        if (mangaId == null) {
            Toast.makeText(context, "Manga ID not found.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack() // Go back if no ID
            return
        }

        viewModel.mangaDetails.observe(viewLifecycleOwner) { manga ->
            manga?.let { displayMangaDetails(it) }
        }
        viewModel.chapters.observe(viewLifecycleOwner) { chapters ->
            chapterListAdapter.submitList(chapters)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingProgressBar.isVisible = isLoading
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.onErrorMessageShown() // Consume the error
            }
        }
        viewModel.navigateToReader.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { pageUrls ->
                if (context != null && pageUrls.isNotEmpty()) { // Check pageUrls not empty
                    val intent = ReaderActivity.newIntent(requireContext(), ArrayList(pageUrls))
                    startActivity(intent)
                } else if (pageUrls.isEmpty()){
                     Toast.makeText(context, "No pages available for this chapter.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.loadMangaData(mangaId)
    }

import android.text.TextUtils // Import TextUtils

// ... (class definition and other methods)

    private fun displayMangaDetails(manga: MangaInfo) {
        titleTextView.text = manga.title

        // Author
        if (!manga.author.isNullOrEmpty() && manga.author != "N/A") {
            authorTextView.text = "Author: ${manga.author}"
            authorTextView.visibility = View.VISIBLE
        } else {
            authorTextView.visibility = View.GONE // Hide if no meaningful author info
        }

        // Status
        if (!manga.status.isNullOrEmpty() && manga.status != "N/A") {
            statusTextView.text = "Status: ${manga.status}"
            statusTextView.visibility = View.VISIBLE
        } else {
            statusTextView.visibility = View.GONE // Hide if no status
        }

        // Description
        if (!manga.description.isNullOrEmpty()) {
            descriptionTextView.text = manga.description
            descriptionTextView.visibility = View.VISIBLE
            descriptionTextView.setOnClickListener { // Simple expand/collapse toggle
                if (descriptionTextView.maxLines == 10) {
                    descriptionTextView.maxLines = Integer.MAX_VALUE
                    descriptionTextView.ellipsize = null
                } else {
                    descriptionTextView.maxLines = 10
                    descriptionTextView.ellipsize = TextUtils.TruncateAt.END
                }
            }
            // Initial state for description based on its content length (optional)
            descriptionTextView.post { // Ensure layout has happened
                if (descriptionTextView.lineCount <= 10) { // If less than maxLines, no need to ellipsize
                    descriptionTextView.maxLines = Integer.MAX_VALUE
                    descriptionTextView.ellipsize = null
                } else {
                    descriptionTextView.maxLines = 10
                    descriptionTextView.ellipsize = TextUtils.TruncateAt.END
                }
            }

        } else {
            descriptionTextView.text = "No description available."
            descriptionTextView.isClickable = false // Disable click if no real description
            descriptionTextView.maxLines = Integer.MAX_VALUE // Show "No description" fully
            descriptionTextView.ellipsize = null
            descriptionTextView.visibility = View.VISIBLE // Ensure it's visible to show "No description"
        }

        coverImageView.load(manga.coverImageUrl) {
            placeholder(R.drawable.ic_launcher_background) // Replace
            error(R.drawable.ic_launcher_foreground)     // Replace
        }
    }

    private fun setupRecyclerView() {
        chapterListAdapter = ChapterListAdapter(
            onChapterClick = { chapter -> viewModel.onChapterClicked(chapter) },
            onDownloadClick = { chapter -> viewModel.downloadChapter(chapter) }
        )
        chaptersRecyclerView.layoutManager = LinearLayoutManager(context)
        chaptersRecyclerView.adapter = chapterListAdapter
        chaptersRecyclerView.isNestedScrollingEnabled = false // Important for RecyclerView in NestedScrollView
    }
}
