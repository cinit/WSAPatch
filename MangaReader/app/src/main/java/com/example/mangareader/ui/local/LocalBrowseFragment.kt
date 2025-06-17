package com.example.mangareader.ui.local

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangareader.R // Ensure R is imported correctly
import android.util.Log // Import Log

// Simple adapter for displaying file/folder names
class DirectoryEntryAdapter(private val entries: List<DocumentFileWrapper>, private val onItemClick: (DocumentFileWrapper) -> Unit) :
    RecyclerView.Adapter<DirectoryEntryAdapter.ViewHolder>() {

    data class DocumentFileWrapper(val name: String?, val uri: Uri, val isDirectory: Boolean, val documentFile: DocumentFile)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1) // Using simple text1 for now
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.textView.text = if (entry.isDirectory) "[D] ${entry.name}" else "[F] ${entry.name}"
        holder.itemView.setOnClickListener { onItemClick(entry) }
    }

    override fun getItemCount() = entries.size
}


class LocalBrowseFragment : Fragment() {

    private lateinit var selectDirectoryButton: Button
    private lateinit var selectedPathTextView: TextView
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var directoryEntryAdapter: DirectoryEntryAdapter
    private var currentUri: Uri? = null

    // Add imports for LocalMangaParser, ReaderActivity, lifecycleScope, kotlinx.coroutines.launch, AlertDialog
    private val localMangaParser = com.example.mangareader.localparser.LocalMangaParser // Access object directly
    private lateinit var readerActivityClass: Class<*> // To avoid direct import if problematic in this context for tools
    init {
        try {
            readerActivityClass = Class.forName("com.example.mangareader.ReaderActivity")
        } catch (e: ClassNotFoundException) {
            // Handle error - this is a fallback for tool environment
            Log.e("LocalBrowseFragment", "ReaderActivity class not found", e)
        }
    }


    private val openDirectoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION // Write if needed
                )
                currentUri = uri
                selectedPathTextView.text = "Selected Directory: ${uri.path}" // This path might not be user-friendly
                Toast.makeText(context, "Directory selected", Toast.LENGTH_LONG).show()
                listFilesInDirectory(uri)
            } catch (e: SecurityException) {
                Toast.makeText(context, "Failed to persist permission: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("LocalBrowseFragment", "Failed to take persistable URI permission.", e)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_local_browse, container, false)
        selectDirectoryButton = view.findViewById(R.id.button_select_directory)
        selectedPathTextView = view.findViewById(R.id.text_view_selected_path)
        filesRecyclerView = view.findViewById(R.id.recycler_view_local_files)

        setupRecyclerView()

        selectDirectoryButton.setOnClickListener {
            openDirectoryLauncher.launch(null)
        }

        // Restore state if currentUri exists (e.g., after configuration change)
        currentUri?.let {
            listFilesInDirectory(it)
            // Update selectedPathTextView with a more persistent name if possible, or just the URI
            val docFile = DocumentFile.fromTreeUri(requireContext(), it)
            selectedPathTextView.text = "Current Path: ${docFile?.name ?: it.path}"
        }

        return view
    }

    private fun setupRecyclerView() {
        directoryEntryAdapter = DirectoryEntryAdapter(emptyList(), this::handleLocalItemClick) // Pass method reference
        filesRecyclerView.layoutManager = LinearLayoutManager(context)
        filesRecyclerView.adapter = directoryEntryAdapter
    }

    private fun listFilesInDirectory(directoryUri: Uri) {
        currentUri = directoryUri
        val docFileRoot = DocumentFile.fromTreeUri(requireContext(), directoryUri)
        selectedPathTextView.text = "Current Path: ${docFileRoot?.name ?: directoryUri.path}" // Update path display

        if (docFileRoot != null && docFileRoot.isDirectory) {
            val entries = docFileRoot.listFiles().mapNotNull { docFile ->
                if (docFile.name == null && docFile.uri.lastPathSegment != null) {
                    DirectoryEntryAdapter.DocumentFileWrapper(docFile.uri.lastPathSegment, docFile.uri, docFile.isDirectory, docFile)
                } else if (docFile.name != null) {
                    DirectoryEntryAdapter.DocumentFileWrapper(docFile.name, docFile.uri, docFile.isDirectory, docFile)
                } else {
                    null
                }
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name?.toLowerCase() }))

            directoryEntryAdapter = DirectoryEntryAdapter(entries) { item ->
                 if (item.isDirectory) {
                    listFilesInDirectory(item.uri)
                } else {
                    // This case is handled by handleLocalItemClick for files
                    // Toast.makeText(context, "Selected file: ${item.name}", Toast.LENGTH_SHORT).show()
                     handleLocalItemClick(item) // if adapter still calls this directly
                }
            }
            filesRecyclerView.adapter = directoryEntryAdapter

        } else {
            Toast.makeText(context, "Failed to list files: Not a directory or access denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLocalItemClick(item: DirectoryEntryAdapter.DocumentFileWrapper) {
        val context = requireContext()
        if (item.isDirectory) {
            // Ask user if they want to view this directory as a chapter OR navigate into it
            android.app.AlertDialog.Builder(context)
                .setTitle(item.name)
                .setMessage("What would you like to do?")
                .setPositiveButton("Read as Chapter") { _, _ ->
                    parseAndOpenManga(item.uri, isCbz = false, isDirectory = true)
                }
                .setNegativeButton("Browse Folder") { _, _ ->
                    listFilesInDirectory(item.uri)
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else { // It's a file
            val fileName = item.name?.lowercase()
            if (fileName != null) {
                when {
                    fileName.endsWith(".cbz") -> {
                        parseAndOpenManga(item.uri, isCbz = true, isDirectory = false)
                    }
                    // else -> Toast.makeText(context, "File type not directly readable as manga.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseAndOpenManga(uri: Uri, isCbz: Boolean, isDirectory: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch { // Use viewLifecycleOwner.lifecycleScope
            var cbzCachePath: String? = null // Variable to store cache path
            try {
                // Show loading indicator if you have one
                Toast.makeText(context, "Parsing manga... please wait.", Toast.LENGTH_SHORT).show()
                val pages: List<com.example.mangareader.localparser.LocalMangaPage> = when {
                    isCbz -> {
                        val result = localMangaParser.parseCbzArchive(requireContext(), uri)
                        cbzCachePath = result.second?.absolutePath // Store cache path
                        result.first
                    }
                    isDirectory -> localMangaParser.parseDirectory(requireContext(), uri)
                    else -> emptyList()
                }

                if (pages.isNotEmpty()) {
                    val pageUris = ArrayList(pages.map { it.uri.toString() })

                    // Using reflection for ReaderActivity.newIntent to avoid direct import if problematic for tool
                    // Assumes newIntent signature: (Context, ArrayList<String>?, String?, Boolean, String?)
                    val newIntentMethod = readerActivityClass.getMethod(
                        "newIntent",
                        Context::class.java, // context
                        ArrayList::class.java, // imageUrls
                        String::class.java, // chapterId
                        Boolean::class.javaPrimitiveType, // isDownloaded (use Boolean.TYPE for primitive)
                        String::class.java  // cbzCacheDirToDelete
                    )
                    val intent = newIntentMethod.invoke(
                        null,
                        requireContext(),
                        pageUris,
                        uri.toString(), // Pseudo-chapterId
                        true, // isDownloaded
                        if (isCbz) cbzCachePath else null // cbzCacheDirToDelete
                    ) as Intent

                    startActivity(intent)

                } else {
                    Toast.makeText(context, "No readable pages found in selection.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("LocalBrowseFragment", "Failed to parse or open manga: ${uri.path}", e)
                Toast.makeText(context, "Error opening manga: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Hide loading indicator
            }
        }
    }

    fun canGoUp(): Boolean {
        currentUri?.let { uri ->
            // Check if it's already the root of the initially selected tree
            // This simple check might not be robust for all cases (e.g. if initial URI wasn't a tree root)
            val initialTreeUriString = openDirectoryLauncher.toString() // This is not the way to get initial URI
                                                                      // We need to store the initial picked URI if we want to prevent going above it.
                                                                      // For now, allow going up as long as parentFile is not null.

            val documentFile = DocumentFile.fromTreeUri(requireContext(), uri)
            return documentFile?.parentFile != null && documentFile.uri != currentUri // Ensure parent is different
        }
        return false
    }

    fun goUp() {
        currentUri?.let { uri ->
            val documentFile = DocumentFile.fromTreeUri(requireContext(), uri)
            documentFile?.parentFile?.let { parentDocFile ->
                // Check if parent is accessible and different from current to prevent loop
                if (parentDocFile.isDirectory && parentDocFile.uri != uri) {
                    listFilesInDirectory(parentDocFile.uri)
                } else {
                    Toast.makeText(context, "Cannot go up further or parent is not a directory.", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(context, "Already at the root of this selection or cannot get parent.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentUri?.let { outState.putParcelable("currentUri", it) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getParcelable<Uri>("currentUri")?.let {
            currentUri = it
            // listFilesInDirectory(it) // listFilesInDirectory is called in onCreateView if currentUri is not null
        }
    }

    companion object {
        fun newInstance() = LocalBrowseFragment()
    }
}
