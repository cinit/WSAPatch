package com.example.mangareader.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangareader.R
import com.example.mangareader.data.model.ChapterInfo
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone // For parsing ISO dates

class ChapterListAdapter(
    private val onChapterClick: (ChapterInfo) -> Unit,
    private val onDownloadClick: (ChapterInfo) -> Unit
) : ListAdapter<ChapterInfo, ChapterListAdapter.ChapterViewHolder>(ChapterDiffCallback()) {

    // Moved SimpleDateFormats to companion object to avoid reinitialization
    companion object {
        private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC") // Assuming publishAt is UTC
        }
        private val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = getItem(position)
        holder.bind(chapter, onChapterClick, onDownloadClick)
    }

    inner class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chapterNumberTextView: TextView = itemView.findViewById(R.id.text_view_chapter_number)
        private val chapterTitleTextView: TextView = itemView.findViewById(R.id.text_view_chapter_title)
        private val scanGroupTextView: TextView = itemView.findViewById(R.id.text_view_chapter_scan_group)
        private val publishDateTextView: TextView = itemView.findViewById(R.id.text_view_chapter_publish_date)
        private val downloadButton: Button = itemView.findViewById(R.id.button_chapter_download)

        fun bind(
            chapter: ChapterInfo,
            onChapterClick: (ChapterInfo) -> Unit,
            onDownloadClick: (ChapterInfo) -> Unit
        ) {
            chapterNumberTextView.text = "Chapter ${chapter.chapterNumber}"
            chapterTitleTextView.text = chapter.title ?: ""
            chapterTitleTextView.visibility = if (chapter.title.isNullOrEmpty()) View.GONE else View.VISIBLE

            scanGroupTextView.text = chapter.scanlationGroup ?: "N/A"
            scanGroupTextView.visibility = if (chapter.scanlationGroup.isNullOrEmpty()) View.GONE else View.VISIBLE

            try {
                chapter.publishAt?.let { dateString ->
                    inputDateFormat.parse(dateString)?.let { date ->
                        publishDateTextView.text = outputDateFormat.format(date)
                        publishDateTextView.visibility = View.VISIBLE
                    } ?: run { publishDateTextView.visibility = View.GONE }
                } ?: run { publishDateTextView.visibility = View.GONE }
            } catch (e: Exception) {
                publishDateTextView.text = chapter.publishAt // Show raw if parsing fails
                publishDateTextView.visibility = if (chapter.publishAt.isNullOrEmpty()) View.GONE else View.VISIBLE
            }


            itemView.setOnClickListener { onChapterClick(chapter) }
            downloadButton.setOnClickListener { onDownloadClick(chapter) }
            // TODO: Update downloadButton text/state based on chapter's download status
        }
    }
}

class ChapterDiffCallback : DiffUtil.ItemCallback<ChapterInfo>() {
    override fun areItemsTheSame(oldItem: ChapterInfo, newItem: ChapterInfo): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: ChapterInfo, newItem: ChapterInfo): Boolean {
        return oldItem == newItem
    }
}
