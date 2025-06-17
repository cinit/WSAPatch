package com.example.mangareader.localparser

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class LocalMangaPage(
    val uri: Uri, // This will be a content URI for files inside archives (after extraction to cache) or direct file URI
    val name: String,
    val type: String? // Mime type if available
)

object LocalMangaParser {
    private const val TAG = "LocalMangaParser"
    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif") // Made public for access from Fragment

    // Parses a directory (assumed to be a chapter) for image files
    suspend fun parseDirectory(context: Context, directoryUri: Uri): List<LocalMangaPage> {
        val documentTree = DocumentFile.fromTreeUri(context, directoryUri)
        val pages = mutableListOf<LocalMangaPage>()
        if (documentTree != null && documentTree.isDirectory) {
            documentTree.listFiles().forEach { docFile ->
                if (docFile.isFile) {
                    val extension = docFile.name?.substringAfterLast('.', "")?.lowercase()
                    if (extension != null && IMAGE_EXTENSIONS.contains(extension)) {
                        pages.add(LocalMangaPage(docFile.uri, docFile.name ?: "Unknown", docFile.type))
                    }
                }
            }
        }
        // Sort pages alphabetically by name (common for manga pages)
        pages.sortBy { it.name }
        Log.d(TAG, "Parsed directory ${directoryUri.path}, found ${pages.size} pages.")
        return pages
    }

    // Parses a CBZ file for image files
    // Note: This extracts images to a temporary cache directory.
    suspend fun parseCbzArchive(context: Context, cbzFileUri: Uri): Pair<List<LocalMangaPage>, File?> { // Changed return type
        val pages = mutableListOf<LocalMangaPage>()
        // Use a more specific name for the cache directory, perhaps based on CBZ file URI hash or name
        val cbzFileName = DocumentFile.fromSingleUri(context, cbzFileUri)?.name ?: "default_cbz_name"
        val cacheDir = File(context.cacheDir, "cbz_temp_${cbzFileName}_${System.currentTimeMillis()}")

        if (!cacheDir.exists()) cacheDir.mkdirs()

        try {
            context.contentResolver.openInputStream(cbzFileUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
                    var zipEntry = zipInputStream.nextEntry
                    while (zipEntry != null) {
                        if (!zipEntry.isDirectory) {
                            val entryName = zipEntry.name
                             if (entryName.contains("..")) {
                                Log.w(TAG, "Skipping potentially malicious zip entry: $entryName")
                                zipEntry = zipInputStream.nextEntry
                                continue
                            }
                            val extension = entryName.substringAfterLast('.', "").lowercase()
                            if (IMAGE_EXTENSIONS.contains(extension)) {
                                val tempFile = File(cacheDir, File(entryName).name)
                                FileOutputStream(tempFile).use { fos ->
                                    zipInputStream.copyTo(fos)
                                }
                                pages.add(LocalMangaPage(Uri.fromFile(tempFile), tempFile.name, context.contentResolver.getType(Uri.fromFile(tempFile))))
                                Log.d(TAG, "Extracted ${tempFile.name} to cache from CBZ.")
                            }
                        }
                        zipInputStream.closeEntry()
                        zipEntry = zipInputStream.nextEntry
                    }
                }
            }
            pages.sortBy { it.name } // Sort before returning
            Log.d(TAG, "Parsed CBZ ${cbzFileUri.path}, found ${pages.size} pages in ${cacheDir.absolutePath}.")
            return Pair(pages, cacheDir) // Return pages and the cache directory
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CBZ file: ${cbzFileUri.path}", e)
            if (cacheDir.exists()) cacheDir.deleteRecursively() // Clean up cache dir on error
            throw e // Re-throw so caller knows it failed and doesn't try to use a null cacheDir path
        }
    }
}
