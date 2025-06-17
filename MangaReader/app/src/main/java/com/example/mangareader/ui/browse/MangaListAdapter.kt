package com.example.mangareader.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView // For loading cover image
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load // For Coil
import com.example.mangareader.R
import com.example.mangareader.data.model.MangaInfo

class MangaListAdapter(
    private var mangaList: List<MangaInfo>,
    private val onMangaItemClicked: (MangaInfo) -> Unit // Changed to pass MangaInfo
) : RecyclerView.Adapter<MangaListAdapter.MangaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manga_list, parent, false) // Ensure item_manga_list has an ImageView
        return MangaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        val manga = mangaList[position]
        holder.bind(manga)
        holder.itemView.setOnClickListener {
            onMangaItemClicked(manga) // Pass the whole MangaInfo object
        }
    }

    override fun getItemCount(): Int = mangaList.size

    fun updateData(newMangaList: List<MangaInfo>) {
        mangaList = newMangaList
        notifyDataSetChanged() // Consider DiffUtil later
    }

    class MangaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_manga_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_manga_author)
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_manga_cover) // Added

        fun bind(manga: MangaInfo) {
            titleTextView.text = manga.title
            authorTextView.text = manga.author ?: "N/A"

            // Load cover image using Coil
            if (!manga.coverImageUrl.isNullOrEmpty()) {
                coverImageView.load(manga.coverImageUrl) {
                    placeholder(R.drawable.ic_launcher_background) // Replace with actual placeholder
                    error(R.drawable.ic_launcher_foreground) // Replace with actual error drawable
                    crossfade(true)
                }
            } else {
                // Set a default placeholder if no cover URL
                coverImageView.setImageResource(R.drawable.ic_launcher_background) // Replace
            }
        }
    }
}
