package com.example.mangareader.ui.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // Import Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangareader.R
import com.example.mangareader.data.local.db.entity.DownloadStatus
import com.example.mangareader.ui.downloads.uimodel.DownloadItemUiModel

class DownloadsAdapter(
    private val onCancelClick: (DownloadItemUiModel) -> Unit,
    private val onRetryClick: (DownloadItemUiModel) -> Unit,
    private val onDeleteClick: (DownloadItemUiModel) -> Unit,
    private val onItemClick: (DownloadItemUiModel) -> Unit // New lambda for item click
) : ListAdapter<DownloadItemUiModel, DownloadsAdapter.DownloadViewHolder>(DownloadItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download_queue, parent, false)
        return DownloadViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class DownloadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mangaTitleTextView: TextView = itemView.findViewById(R.id.text_view_download_manga_title)
        private val chapterTitleTextView: TextView = itemView.findViewById(R.id.text_view_download_chapter_title)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar_download_item)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_view_download_status)
        private val cancelButton: Button = itemView.findViewById(R.id.button_download_action_cancel)
        private val retryButton: Button = itemView.findViewById(R.id.button_download_action_retry)
        private val deleteButton: Button = itemView.findViewById(R.id.button_download_action_delete)

        fun bind(item: DownloadItemUiModel) {
            mangaTitleTextView.text = item.mangaTitle
            chapterTitleTextView.text = item.chapterTitle
            progressBar.progress = item.progress

            val isActive = item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.QUEUED
            progressBar.visibility = if (isActive) View.VISIBLE else View.GONE

            statusTextView.text = "Status: ${item.status.name}${if (item.workInfoState != null && item.workInfoState != "SUCCEEDED" && item.workInfoState != "FAILED" && item.workInfoState != "CANCELLED" &&  item.workInfoState != "BLOCKED") " (${item.workInfoState})" else ""} - ${item.progress}%"


            // Manage button visibility based on status
            cancelButton.visibility = if (item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.DOWNLOADING) View.VISIBLE else View.GONE
            retryButton.visibility = if (item.status == DownloadStatus.FAILED || item.status == DownloadStatus.PAUSED) View.VISIBLE else View.GONE // Allow retry on PAUSED too
            deleteButton.visibility = if (item.status == DownloadStatus.DOWNLOADED || item.status == DownloadStatus.FAILED || item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.NOT_DOWNLOADED) View.VISIBLE else View.GONE

            cancelButton.setOnClickListener { onCancelClick(item) }
            retryButton.setOnClickListener { onRetryClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }

            itemView.setOnClickListener { // Make the item itself clickable
                if (item.status == DownloadStatus.DOWNLOADED) { // Only if downloaded
                    onItemClick(item)
                } else {
                    // Optional: Toast or message indicating chapter not downloaded
                    android.widget.Toast.makeText(itemView.context, "Chapter not yet downloaded.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class DownloadItemDiffCallback : DiffUtil.ItemCallback<DownloadItemUiModel>() {
    override fun areItemsTheSame(oldItem: DownloadItemUiModel, newItem: DownloadItemUiModel): Boolean {
        return oldItem.chapterId == newItem.chapterId
    }
    override fun areContentsTheSame(oldItem: DownloadItemUiModel, newItem: DownloadItemUiModel): Boolean {
        return oldItem == newItem
    }
}
