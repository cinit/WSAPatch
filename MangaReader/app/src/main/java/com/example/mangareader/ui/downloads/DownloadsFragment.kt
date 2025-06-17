package com.example.mangareader.ui.downloads

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangareader.R

class DownloadsFragment : Fragment() {
    private val viewModel: DownloadsViewModel by viewModels()
    private lateinit var downloadsAdapter: DownloadsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_downloads, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_downloads)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        viewModel.downloadItems.observe(viewLifecycleOwner) { items ->
            downloadsAdapter.submitList(items)
        }
    }

    private fun setupRecyclerView() {
        downloadsAdapter = DownloadsAdapter(
            onCancelClick = { item -> viewModel.cancelDownload(item.chapterId) },
            onRetryClick = { item -> viewModel.retryDownload(item) },
            onDeleteClick = { item ->
                // Optional: Add a confirmation dialog before deleting
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Download")
                    .setMessage("Are you sure you want to delete the download for ${item.mangaTitle} - ${item.chapterTitle}?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteChapter(item.chapterId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onItemClick = { item -> // New: Handle item click
                if (item.status == com.example.mangareader.data.local.db.entity.DownloadStatus.DOWNLOADED) {
                    val intent = com.example.mangareader.ReaderActivity.newIntent(
                        context = requireContext(),
                        chapterId = item.chapterId,
                        isDownloaded = true
                    )
                    startActivity(intent)
                }
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = downloadsAdapter
        }
    }

    companion object {
        fun newInstance() = DownloadsFragment()
    }
}
