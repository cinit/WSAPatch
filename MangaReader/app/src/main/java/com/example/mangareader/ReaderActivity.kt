package com.example.mangareader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.mangareader.data.local.db.AppDatabase
import com.example.mangareader.ui.reader.MangaPagerAdapter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
// import java.util.Collections // Not strictly needed if using .reversed() and new ArrayList

class ReaderActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var originalPageUris: List<String> = emptyList()
    private var currentDisplayPageUris: List<String> = emptyList()
    private var cbzCachePathToDelete: String? = null
    private lateinit var pageNumberTextView: TextView
    private lateinit var toggleDirectionButton: Button

    private var isRtl: Boolean = false // Default LTR

    companion object {
        const val EXTRA_IMAGE_URLS = "extra_image_urls"
        const val EXTRA_CHAPTER_ID = "extra_chapter_id"
        const val EXTRA_IS_DOWNLOADED = "extra_is_downloaded"
        const val EXTRA_CBZ_CACHE_DIR_TO_DELETE = "extra_cbz_cache_dir_to_delete"

        fun newIntent(
            context: Context,
            imageUrls: ArrayList<String>? = null,
            chapterId: String? = null,
            isDownloaded: Boolean = false,
            cbzCacheDirToDelete: String? = null
        ): Intent {
            val intent = Intent(context, ReaderActivity::class.java)
            if (imageUrls != null) {
                intent.putStringArrayListExtra(EXTRA_IMAGE_URLS, imageUrls)
            }
            if (chapterId != null) {
                intent.putExtra(EXTRA_CHAPTER_ID, chapterId)
            }
            intent.putExtra(EXTRA_IS_DOWNLOADED, isDownloaded)
            if (cbzCacheDirToDelete != null) {
                intent.putExtra(EXTRA_CBZ_CACHE_DIR_TO_DELETE, cbzCacheDirToDelete)
            }
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        cbzCachePathToDelete = intent.getStringExtra(EXTRA_CBZ_CACHE_DIR_TO_DELETE)
        viewPager = findViewById(R.id.view_pager_manga)
        pageNumberTextView = findViewById(R.id.text_view_page_number)
        toggleDirectionButton = findViewById(R.id.button_toggle_reading_direction)

        // TODO: Load isRtl from SharedPreferences if saved previously

        lifecycleScope.launch {
            val isDownloadedFromIntent = intent.getBooleanExtra(EXTRA_IS_DOWNLOADED, false)
            val chapterIdForOffline = intent.getStringExtra(EXTRA_CHAPTER_ID)
            var tempUris: List<String> = emptyList()

            if (isDownloadedFromIntent && chapterIdForOffline != null) {
                if (cbzCachePathToDelete != null) { // CBZ case
                    tempUris = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: emptyList()
                    Log.d("ReaderActivity", "Loading CBZ from cache with ${tempUris.size} pages. Cache dir: $cbzCachePathToDelete")
                } else { // Downloaded chapter from DB case
                    val chapterDao = AppDatabase.getDatabase(applicationContext).downloadedChapterDao()
                    val chapterEntity = chapterDao.getChapterById(chapterIdForOffline).firstOrNull()
                    if (chapterEntity?.chapterDownloadPath != null) {
                        val chapterDir = File(chapterEntity.chapterDownloadPath)
                        if (chapterDir.exists() && chapterDir.isDirectory) {
                            val imageFiles = chapterDir.listFiles { file ->
                                file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png") || file.name.endsWith(".jpeg") || file.name.endsWith(".webp"))
                            }?.sortedBy { file ->
                                file.nameWithoutExtension.substringAfterLast('_').toIntOrNull() ?: 0
                            }
                            tempUris = imageFiles?.map { Uri.fromFile(it).toString() } ?: emptyList()
                            Log.d("ReaderActivity", "Loading downloaded chapter $chapterIdForOffline with ${tempUris.size} pages from ${chapterDir.absolutePath}")
                        } else {
                            Log.e("ReaderActivity", "Chapter directory not found or not a directory: ${chapterEntity.chapterDownloadPath}")
                        }
                    } else {
                        Log.e("ReaderActivity", "Chapter entity or download path not found for $chapterIdForOffline")
                    }
                }
            } else { // Online case
                tempUris = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: emptyList()
                Log.d("ReaderActivity", "Loading online chapter with ${tempUris.size} pages.")
            }

            originalPageUris = ArrayList(tempUris) // Make a copy

            if (originalPageUris.isNotEmpty()) {
                setupViewPagerAndDirection()
            } else {
                Toast.makeText(this@ReaderActivity, "No pages to display.", Toast.LENGTH_LONG).show()
                pageNumberTextView.text = "0 / 0"
                finish()
            }
        }

        toggleDirectionButton.setOnClickListener {
            val currentLogicalPageBeforeToggle = getCurrentLogicalPage()
            isRtl = !isRtl
            // TODO: Save isRtl to SharedPreferences if needed

            currentDisplayPageUris = if (isRtl) ArrayList(originalPageUris.reversed()) else ArrayList(originalPageUris)
            viewPager.adapter = MangaPagerAdapter(this, currentDisplayPageUris) // Reset adapter

            val newTotalPages = currentDisplayPageUris.size
            val newPhysicalIndex = if (isRtl) {
                newTotalPages - currentLogicalPageBeforeToggle
            } else {
                currentLogicalPageBeforeToggle - 1
            }

            if (newPhysicalIndex >= 0 && newPhysicalIndex < newTotalPages) {
                 viewPager.setCurrentItem(newPhysicalIndex, false)
            } else if (newTotalPages > 0) { // Fallback to first page if index is out of bounds
                viewPager.setCurrentItem(0, false)
            }

            updateDirectionButtonText()
            updatePageNumberDisplay(viewPager.currentItem) // Update based on new current item
            viewPager.layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
    }

    private fun setupViewPagerAndDirection() {
        currentDisplayPageUris = if (isRtl) ArrayList(originalPageUris.reversed()) else ArrayList(originalPageUris)
        viewPager.adapter = MangaPagerAdapter(this@ReaderActivity, currentDisplayPageUris)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageNumberDisplay(position)
            }
        })

        updateDirectionButtonText()
        updatePageNumberDisplay(viewPager.currentItem) // Initial display
        viewPager.layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
    }

    private fun updateDirectionButtonText() {
        toggleDirectionButton.text = if (isRtl) "RTL" else "LTR"
    }

    private fun getCurrentLogicalPage(): Int {
        val physicalIndex = viewPager.currentItem
        return if (isRtl) {
            currentDisplayPageUris.size - physicalIndex
        } else {
            physicalIndex + 1
        }
    }

    private fun updatePageNumberDisplay(currentPhysicalPositionInAdapter: Int) {
        if (originalPageUris.isEmpty()) {
            pageNumberTextView.text = "0 / 0"
            return
        }
        val totalPages = originalPageUris.size
        val currentLogicalPage = if (isRtl) {
            totalPages - currentPhysicalPositionInAdapter
        } else {
            currentPhysicalPositionInAdapter + 1
        }
        // Ensure logical page is within bounds, especially during transitions
        val displayPage = currentLogicalPage.coerceIn(1, totalPages.coerceAtLeast(1))
        pageNumberTextView.text = "$displayPage / $totalPages"
    }

    override fun onDestroy() {
        super.onDestroy()
        cbzCachePathToDelete?.let { path ->
            Thread {
                Log.d("ReaderActivity", "Attempting to delete CBZ cache directory: $path")
                val cacheDir = File(path)
                if (cacheDir.exists()) {
                    if (cacheDir.deleteRecursively()) {
                        Log.i("ReaderActivity", "Successfully deleted CBZ cache directory: $path")
                    } else {
                        Log.e("ReaderActivity", "Failed to delete CBZ cache directory: $path")
                    }
                }
            }.start()
        }
    }
}
