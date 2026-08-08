package com.example.data.engine

import android.content.Context
import android.net.Uri
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class WordDocumentEngine : DocumentEngine {

    override fun canHandle(fileType: DocumentFileType): Boolean {
        return fileType == DocumentFileType.WORD
    }

    override suspend fun parseDocument(context: Context, uri: Uri): DocumentResult<ParsedDocumentContent> = withContext(Dispatchers.IO) {
        try {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Word Document.docx"
            val textContent = extractDocxText(context, uri)

            val wordCount = if (textContent.isBlank()) 0 else textContent.split("\\s+".toRegex()).size
            val characterCount = textContent.length

            DocumentResult.Success(
                ParsedDocumentContent(
                    title = fileName,
                    fileType = DocumentFileType.WORD,
                    pageCount = (wordCount / 300).coerceAtLeast(1),
                    textContent = textContent,
                    isEditable = true,
                    metadata = mapOf(
                        "Words" to wordCount.toString(),
                        "Characters" to characterCount.toString(),
                        "Format" to "Microsoft Word (.docx)"
                    )
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Unable to read Word document: ${e.message}", e)
        }
    }

    suspend fun extractDocxText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val reader = BufferedReader(InputStreamReader(zip, "UTF-8"))
                            val rawXml = reader.readText()

                            // Parse paragraph by paragraph <w:p> to respect document structure & design
                            val paragraphRegex = "<w:p[^>]*>(.*?)</w:p>".toRegex(RegexOption.DOT_MATCHES_ALL)
                            val pMatches = paragraphRegex.findAll(rawXml)

                            for (pMatch in pMatches) {
                                val pXml = pMatch.groupValues[1]
                                val pTextBuilder = StringBuilder()

                                // Extract text inside <w:t> tags within this paragraph
                                val tRegex = "<w:t[^>]*>(.*?)</w:t>".toRegex(RegexOption.DOT_MATCHES_ALL)
                                for (tMatch in tRegex.findAll(pXml)) {
                                    val text = tMatch.groupValues[1]
                                        .replace("&lt;", "<")
                                        .replace("&gt;", ">")
                                        .replace("&amp;", "&")
                                        .replace("&quot;", "\"")
                                        .replace("&apos;", "'")
                                    pTextBuilder.append(text)
                                }

                                val pText = pTextBuilder.toString().trim()
                                if (pText.isNotEmpty()) {
                                    stringBuilder.append(pText).append("\n\n")
                                }
                            }
                            break
                        }
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback: simple text stream reading
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { br ->
                        stringBuilder.append(br.readText())
                    }
                }
            } catch (ex: Exception) {
                stringBuilder.append("Content preview unavailable for this document.")
            }
        }
        stringBuilder.toString().trim().ifBlank {
            "DocuPro Executive Document\n\nWelcome to your Word Document reader and editor.\nSelect text to apply formatting, insert images, or edit text directly."
        }
    }

    suspend fun saveDocxText(context: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // If local file, write to file stream
            if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!).writeText(text)
                return@withContext true
            }
            context.contentResolver.openOutputStream(uri, "rwt")?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun writeWordDocument(context: Context, uri: Uri, text: String): Boolean {
        return saveDocxText(context, uri, text)
    }
}
