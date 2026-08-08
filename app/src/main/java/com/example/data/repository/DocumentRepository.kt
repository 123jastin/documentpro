package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.AnnotationDao
import com.example.data.db.DocumentDao
import com.example.data.db.ScanDao
import com.example.data.engine.PdfDocumentEngine
import com.example.data.engine.PresentationEngine
import com.example.data.engine.SpreadsheetEngine
import com.example.data.engine.TextDocumentEngine
import com.example.data.engine.WordDocumentEngine
import com.example.data.model.AnnotationType
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.data.model.PdfAnnotation
import com.example.data.model.ScanItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val annotationDao: AnnotationDao,
    private val scanDao: ScanDao
) {

    val allDocuments: Flow<List<DocumentItem>> = documentDao.getAllDocuments()
    val recentDocuments: Flow<List<DocumentItem>> = documentDao.getRecentDocuments()
    val starredDocuments: Flow<List<DocumentItem>> = documentDao.getStarredDocuments()
    val trashDocuments: Flow<List<DocumentItem>> = documentDao.getTrashDocuments()

    fun getDocumentsByCategory(type: DocumentFileType): Flow<List<DocumentItem>> {
        return documentDao.getDocumentsByType(type)
    }

    fun searchDocuments(query: String): Flow<List<DocumentItem>> {
        return documentDao.searchDocuments(query)
    }

    suspend fun getDocumentByUri(uri: String): DocumentItem? {
        return documentDao.getDocumentByUri(uri)
    }

    suspend fun toggleStar(uri: String, currentStarred: Boolean) {
        documentDao.setStarred(uri, !currentStarred)
    }

    suspend fun updateReadingProgress(uri: String, lastPage: Int) {
        documentDao.updateReadingProgress(uri, System.currentTimeMillis(), lastPage)
    }

    suspend fun renameDocument(uri: String, newName: String) {
        documentDao.renameDocument(uri, newName)
    }

    suspend fun moveToTrash(uri: String) {
        documentDao.moveToTrash(uri)
    }

    suspend fun deletePermanently(uri: String) {
        documentDao.deleteDocumentPermanently(uri)
    }

    suspend fun emptyTrash() {
        documentDao.emptyTrash()
    }

    // PDF Annotations
    fun getAnnotationsForDocument(uri: String): Flow<List<PdfAnnotation>> {
        return annotationDao.getAnnotationsForDocument(uri)
    }

    suspend fun addAnnotation(annotation: PdfAnnotation): Long {
        return annotationDao.insertAnnotation(annotation)
    }

    suspend fun deleteAnnotation(annotation: PdfAnnotation) {
        annotationDao.deleteAnnotation(annotation)
    }

    // Scans
    fun getScanPages(sessionId: String): Flow<List<ScanItem>> {
        return scanDao.getScanPages(sessionId)
    }

    suspend fun addScanPage(page: ScanItem): Long {
        return scanDao.insertScanPage(page)
    }

    suspend fun deleteScanPage(page: ScanItem) {
        scanDao.deleteScanPage(page)
    }

    suspend fun clearScanSession(sessionId: String) {
        scanDao.clearScanSession(sessionId)
    }

    suspend fun insertDocument(document: DocumentItem) {
        documentDao.insertDocument(document)
    }

    suspend fun ensureSampleDocumentsExist() = withContext(Dispatchers.IO) {
        scanDeviceDocuments()
    }

    suspend fun scanDeviceDocuments(): List<DocumentItem> = withContext(Dispatchers.IO) {
        val scannedDocs = mutableListOf<DocumentItem>()
        val foundUris = mutableSetOf<String>()

        // 1. Query MediaStore.Files for documents on the phone
        try {
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
            } else {
                android.provider.MediaStore.Files.getContentUri("external")
            }

            val projection = arrayOf(
                android.provider.MediaStore.Files.FileColumns._ID,
                android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
                android.provider.MediaStore.Files.FileColumns.DATA,
                android.provider.MediaStore.Files.FileColumns.SIZE,
                android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val selection = "(" +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pdf' OR " +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.doc' OR " +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.docx' OR " +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.xls' OR " +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.xlsx' OR " +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.csv' OR " +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.ppt' OR " +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pptx' OR " +
                    "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.txt'" +
                    ")"

            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndex(android.provider.MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Document"
                    val filePath = if (dataColumn != -1) cursor.getString(dataColumn) else null
                    val size = cursor.getLong(sizeColumn)
                    val dateMod = cursor.getLong(dateColumn) * 1000L

                    val contentUri = android.content.ContentUris.withAppendedId(collection, id)
                    val uriStr = contentUri.toString()

                    if (!foundUris.contains(uriStr)) {
                        foundUris.add(uriStr)
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val fileType = when (ext) {
                            "pdf" -> DocumentFileType.PDF
                            "doc", "docx" -> DocumentFileType.WORD
                            "xls", "xlsx", "csv" -> DocumentFileType.EXCEL
                            "ppt", "pptx" -> DocumentFileType.POWERPOINT
                            "txt" -> DocumentFileType.TEXT
                            else -> DocumentFileType.OTHER
                        }

                        scannedDocs.add(
                            DocumentItem(
                                uriString = uriStr,
                                displayName = name,
                                extension = ext,
                                fileType = fileType,
                                sizeBytes = size,
                                dateModified = if (dateMod > 0) dateMod else System.currentTimeMillis(),
                                filePath = filePath ?: ""
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Direct folder scan in public & app storage directories
        val targetDirs = listOfNotNull(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(null),
            context.filesDir
        )

        val validExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "csv", "ppt", "pptx", "txt")

        for (dir in targetDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().maxDepth(3).filter { file ->
                        file.isFile && file.extension.lowercase() in validExtensions
                    }.forEach { file ->
                        val uriStr = Uri.fromFile(file).toString()
                        if (!foundUris.contains(uriStr)) {
                            foundUris.add(uriStr)
                            val ext = file.extension.lowercase()
                            val fileType = when (ext) {
                                "pdf" -> DocumentFileType.PDF
                                "doc", "docx" -> DocumentFileType.WORD
                                "xls", "xlsx", "csv" -> DocumentFileType.EXCEL
                                "ppt", "pptx" -> DocumentFileType.POWERPOINT
                                "txt" -> DocumentFileType.TEXT
                                else -> DocumentFileType.OTHER
                            }
                            scannedDocs.add(
                                DocumentItem(
                                    uriString = uriStr,
                                    displayName = file.name,
                                    extension = ext,
                                    fileType = fileType,
                                    sizeBytes = file.length(),
                                    dateModified = file.lastModified(),
                                    filePath = file.absolutePath
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (scannedDocs.isNotEmpty()) {
            documentDao.insertDocuments(scannedDocs)
        }
        return@withContext scannedDocs
    }

    suspend fun importDocumentFromUri(uri: Uri): DocumentItem = withContext(Dispatchers.IO) {
        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document"
        var sizeBytes = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val ext = displayName.substringAfterLast('.', "").lowercase()
        val fileType = when (ext) {
            "pdf" -> DocumentFileType.PDF
            "doc", "docx" -> DocumentFileType.WORD
            "xls", "xlsx", "csv" -> DocumentFileType.EXCEL
            "ppt", "pptx" -> DocumentFileType.POWERPOINT
            "txt" -> DocumentFileType.TEXT
            else -> DocumentFileType.OTHER
        }

        val item = DocumentItem(
            uriString = uri.toString(),
            displayName = displayName,
            extension = ext,
            fileType = fileType,
            sizeBytes = sizeBytes,
            dateModified = System.currentTimeMillis(),
            isRecent = true,
            lastOpenedTime = System.currentTimeMillis()
        )

        documentDao.insertDocument(item)
        return@withContext item
    }
}
