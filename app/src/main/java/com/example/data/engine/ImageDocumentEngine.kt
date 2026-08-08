package com.example.data.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageDocumentEngine : DocumentEngine {

    override fun canHandle(fileType: DocumentFileType): Boolean {
        return fileType == DocumentFileType.IMAGE
    }

    override suspend fun parseDocument(context: Context, uri: Uri): DocumentResult<ParsedDocumentContent> = withContext(Dispatchers.IO) {
        try {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Image.jpg"
            val bitmap = loadBitmap(context, uri)

            val dimensions = if (bitmap != null) "${bitmap.width} x ${bitmap.height} px" else "Unknown"

            DocumentResult.Success(
                ParsedDocumentContent(
                    title = fileName,
                    fileType = DocumentFileType.IMAGE,
                    pageCount = 1,
                    textContent = "Image Document ($dimensions)",
                    isEditable = false,
                    metadata = mapOf(
                        "Dimensions" to dimensions,
                        "Format" to "Image"
                    )
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Failed to load image: ${e.message}", e)
        }
    }

    suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
