package com.example.mangareader.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// Remove Button import if not used
// Remove ImageView import, use PhotoView
import androidx.fragment.app.Fragment
import coil.load
import com.example.mangareader.R
import com.github.chrisbanes.photoview.PhotoView // Import PhotoView

class PageFragment : Fragment() {

    private lateinit var pageUriString: String
    // private lateinit var imageView: ImageView // Old
    private lateinit var photoView: PhotoView // New

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // imageUrl = it.getString(ARG_IMAGE_URL) ?: "" // Old
            pageUriString = it.getString(ARG_PAGE_URI_STRING) ?: "" // New key
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_page, container, false)
        // imageView = view.findViewById(R.id.image_view_page) // Old
        photoView = view.findViewById(R.id.photoview_page) // New

        // Remove zoom button findViewById and listeners
        // val zoomInButton = view.findViewById<Button>(R.id.button_zoom_in)
        // val zoomOutButton = view.findViewById<Button>(R.id.button_zoom_out)
        // ... (remove listeners) ...

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (pageUriString.isNotEmpty()) {
            // photoView.load(pageUriString) { // Coil extension for PhotoView might not exist directly
            // Load into PhotoView using Coil
            photoView.load(pageUriString) {
                placeholder(R.drawable.ic_launcher_background) // Replace
                error(R.drawable.ic_launcher_foreground)     // Replace
                // For PhotoView, you might want to listen for success to update the display matrix if needed,
                // but usually, it handles it well once the drawable is set.
                listener(
                    onSuccess = { _, _ -> photoView.attacher.update() },
                    onError = { _, _ -> /* Handle error if needed */ }
                )
            }
        }
    }

    companion object {
        // private const val ARG_IMAGE_URL = "image_url" // Old
        private const val ARG_PAGE_URI_STRING = "page_uri_string" // New key

        // fun newInstance(imageUrl: String): PageFragment { // Old
        fun newInstance(pageUriString: String): PageFragment { // New
            val fragment = PageFragment()
            val args = Bundle()
            // args.putString(ARG_IMAGE_URL, imageUrl) // Old
            args.putString(ARG_PAGE_URI_STRING, pageUriString) // New
            fragment.arguments = args
            return fragment
        }
    }
}
