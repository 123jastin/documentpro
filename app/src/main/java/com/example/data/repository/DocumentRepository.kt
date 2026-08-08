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

    // Populate initial realistic sample documents if database is empty on first run
    suspend fun ensureSampleDocumentsExist() = withContext(Dispatchers.IO) {
        val currentDocs = allDocuments.first()
        if (currentDocs.isNotEmpty()) return@withContext

        val docsDir = File(context.filesDir, "documents")
        if (!docsDir.exists()) docsDir.mkdirs()

        val pdfEngine = PdfDocumentEngine()
        val samplePdf = File(docsDir, "Annual_Report_2026.pdf")
        if (!samplePdf.exists()) {
            pdfEngine.createBlankPdf(samplePdf, pageCount = 6)
        }

        val wordFile = File(docsDir, "Project_Proposal_DocuPro.docx")
        if (!wordFile.exists()) {
            wordFile.writeText("DocuPro Project Proposal & Roadmap\n\n1. Executive Overview\nDocuPro delivers enterprise-grade native Android document processing, PDF annotations, formatted Office document reading, and camera scanning.\n\n2. Key Objectives\n• Ultra-fast PDF page rendering\n• Real local Room database persistence for recents & favorites\n• Offline document scanner with edge filters and text recognition\n\n3. Deliverables & Acceptance Criteria\n• Zero crash tolerance\n• Edge-to-edge Material 3 interface\n• 100% privacy with zero unauthorized network telemetry.")
        }

        val excelFile = File(docsDir, "Q3_Financial_Budget.csv")
        if (!excelFile.exists()) {
            excelFile.writeText("Department,Q1 Budget ($),Q2 Budget ($),Q3 Budget ($),Status\nEngineering,125000,132000,140000,Approved\nDesign,35000,38000,42000,Approved\nMarketing,45000,52000,60000,Pending\nOperations,28000,30000,31000,Approved\nLegal & IP,15000,12000,18000,Approved\nTotal,248000,264000,291000,In Review")
        }

        val pptFile = File(docsDir, "Executive_Strategy_Deck.pptx")
        if (!pptFile.exists()) {
            pptFile.writeText("Executive Strategy Deck Slide 1 Title: DocuPro Overview Slide 2 Title: Q3 Growth Milestones Slide 3 Title: Architecture Summary")
        }

        val txtFile = File(docsDir, "Meeting_Notes_Aug2026.txt")
        if (!txtFile.exists()) {
            txtFile.writeText("Meeting Notes - August 8, 2026\n\nAttendees: Alex, Sarah, David, Elena\n\nKey Decisions:\n1. Approved new Deep Blue color theme and Material 3 design spec for DocuPro.\n2. Finalized offline-first storage architecture using Room and Storage Access Framework.\n3. Verified CameraX scanner with automatic brightness, contrast, and B&W filters.\n\nNext Steps:\n- Complete PDF annotation drawing canvas test suite.\n- Perform benchmark build verification with compile_applet.")
        }

        val now = System.currentTimeMillis()
        val samples = listOf(
            DocumentItem(
                uriString = Uri.fromFile(samplePdf).toString(),
                displayName = "Annual_Report_2026.pdf",
                extension = "pdf",
                fileType = DocumentFileType.PDF,
                sizeBytes = samplePdf.length(),
                dateModified = now - 3600000,
                isStarred = true,
                isRecent = true,
                lastOpenedTime = now - 3600000,
                pageCount = 6,
                filePath = samplePdf.absolutePath,
                contentSummary = "Annual Financial & Operations Report 2026"
            ),
            DocumentItem(
                uriString = Uri.fromFile(wordFile).toString(),
                displayName = "Project_Proposal_DocuPro.docx",
                extension = "docx",
                fileType = DocumentFileType.WORD,
                sizeBytes = wordFile.length(),
                dateModified = now - 7200000,
                isStarred = true,
                isRecent = true,
                lastOpenedTime = now - 7200000,
                pageCount = 3,
                filePath = wordFile.absolutePath,
                contentSummary = "DocuPro Project Proposal & Feature Specifications"
            ),
            DocumentItem(
                uriString = Uri.fromFile(excelFile).toString(),
                displayName = "Q3_Financial_Budget.csv",
                extension = "csv",
                fileType = DocumentFileType.EXCEL,
                sizeBytes = excelFile.length(),
                dateModified = now - 14400000,
                isStarred = false,
                isRecent = true,
                lastOpenedTime = now - 14400000,
                pageCount = 1,
                filePath = excelFile.absolutePath,
                contentSummary = "Department Budget Breakdown for Q1, Q2, Q3"
            ),
            DocumentItem(
                uriString = Uri.fromFile(pptFile).toString(),
                displayName = "Executive_Strategy_Deck.pptx",
                extension = "pptx",
                fileType = DocumentFileType.POWERPOINT,
                sizeBytes = pptFile.length(),
                dateModified = now - 28800000,
                isStarred = false,
                isRecent = true,
                lastOpenedTime = now - 28800000,
                pageCount = 4,
                filePath = pptFile.absolutePath,
                contentSummary = "DocuPro Strategy Deck and Growth Roadmap"
            ),
            DocumentItem(
                uriString = Uri.fromFile(txtFile).toString(),
                displayName = "Meeting_Notes_Aug2026.txt",
                extension = "txt",
                fileType = DocumentFileType.TEXT,
                sizeBytes = txtFile.length(),
                dateModified = now - 43200000,
                isStarred = true,
                isRecent = true,
                lastOpenedTime = now - 43200000,
                pageCount = 1,
                filePath = txtFile.absolutePath,
                contentSummary = "Team Meeting Minutes and Key Action Items"
            )
        )

        documentDao.insertDocuments(samples)
    }
}
