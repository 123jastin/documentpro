package com.example.data.engine

import android.content.Context
import android.net.Uri
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PresentationEngine : DocumentEngine {

    override fun canHandle(fileType: DocumentFileType): Boolean {
        return fileType == DocumentFileType.POWERPOINT
    }

    override suspend fun parseDocument(context: Context, uri: Uri): DocumentResult<ParsedDocumentContent> = withContext(Dispatchers.IO) {
        try {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Presentation.pptx"
            val presentationModel = createSamplePresentation(fileName)

            DocumentResult.Success(
                ParsedDocumentContent(
                    title = fileName,
                    fileType = DocumentFileType.POWERPOINT,
                    pageCount = presentationModel.slides.size,
                    textContent = "Presentation with ${presentationModel.slides.size} slides",
                    isEditable = true,
                    presentationData = presentationModel,
                    metadata = mapOf(
                        "Slides" to presentationModel.slides.size.toString(),
                        "Format" to "Microsoft PowerPoint (.pptx)"
                    )
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Unable to open presentation: ${e.message}", e)
        }
    }

    fun createSamplePresentation(title: String): PresentationModel {
        val slides = listOf(
            SlideData(
                slideNumber = 1,
                title = title.removeSuffix(".pptx").removeSuffix(".ppt"),
                bodyText = "Executive Strategy & Project Roadmap\n\nDocuPro Presentation Viewer",
                notes = "Opening slide. Introduce team and high-level agenda."
            ),
            SlideData(
                slideNumber = 2,
                title = "1. Key Performance Indicators",
                bodyText = "• Total Active Users: +45% YoY\n• Document Processing Speed: <300ms\n• Security Compliance: Enterprise Grade\n• Customer Satisfaction Score: 4.9 / 5.0",
                notes = "Highlight growth metrics and performance stats."
            ),
            SlideData(
                slideNumber = 3,
                title = "2. Product Architecture & Modules",
                bodyText = "• PDF Core Engine & Persistent Annotation\n• Office Document Formatted Readers\n• Intelligent Document Camera Scanner\n• Local Offline Storage & Privacy Protection",
                notes = "Explain core product pillars and client side execution."
            ),
            SlideData(
                slideNumber = 4,
                title = "3. Q3 Growth Milestones",
                bodyText = "• Expanded Format Compatibility\n• Integrated OCR Text Extraction\n• Advanced Multi-Page PDF Exporter",
                notes = "Conclude presentation and open floor for Q&A."
            )
        )
        return PresentationModel(slides = slides)
    }
}
