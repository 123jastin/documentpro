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

class TextDocumentEngine : DocumentEngine {

    override fun canHandle(fileType: DocumentFileType): Boolean {
        return fileType == DocumentFileType.TEXT
    }

    override suspend fun parseDocument(context: Context, uri: Uri): DocumentResult<ParsedDocumentContent> = withContext(Dispatchers.IO) {
        try {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.txt"
            val textContent = readTextFromUri(context, uri)

            val lineCount = textContent.lines().size
            val wordCount = if (textContent.isBlank()) 0 else textContent.split("\\s+".toRegex()).size

            DocumentResult.Success(
                ParsedDocumentContent(
                    title = fileName,
                    fileType = DocumentFileType.TEXT,
                    pageCount = (lineCount / 40).coerceAtLeast(1),
                    textContent = textContent,
                    isEditable = true,
                    metadata = mapOf(
                        "Lines" to lineCount.toString(),
                        "Words" to wordCount.toString(),
                        "Encoding" to "UTF-8"
                    )
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Unable to read text file: ${e.message}", e)
        }
    }

    suspend fun readTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        try {
            if (uri.scheme == "file" && uri.path != null) {
                return@withContext File(uri.path!!).readText()
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stringBuilder.toString()
    }

    suspend fun saveTextToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!).writeText(content)
                return@withContext true
            }
            context.contentResolver.openOutputStream(uri, "rwt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
