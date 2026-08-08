package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ColorExcelGreen
import com.example.ui.theme.ColorImagePurple
import com.example.ui.theme.ColorPdfRed
import com.example.ui.theme.ColorPptOrange
import com.example.ui.theme.ColorTextGray
import com.example.ui.theme.ColorWordBlue

enum class DocumentFileType(
    val extensionName: String,
    val mimeType: String,
    val categoryName: String,
    val brandColor: Color
) {
    PDF("PDF", "application/pdf", "PDFs", ColorPdfRed),
    WORD("DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word", ColorWordBlue),
    EXCEL("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel", ColorExcelGreen),
    POWERPOINT("PPTX", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "PowerPoint", ColorPptOrange),
    TEXT("TXT", "text/plain", "Text", ColorTextGray),
    IMAGE("IMAGE", "image/*", "Images", ColorImagePurple),
    OTHER("FILE", "*/*", "Other", ColorTextGray);

    companion object {
        fun fromExtension(extension: String): DocumentFileType {
            val ext = extension.lowercase().removePrefix(".")
            return when (ext) {
                "pdf" -> PDF
                "doc", "docx", "odt", "rtf" -> WORD
                "xls", "xlsx", "ods", "csv" -> EXCEL
                "ppt", "pptx", "odp" -> POWERPOINT
                "txt", "log", "md", "json", "xml" -> TEXT
                "jpg", "jpeg", "png", "webp", "heic", "gif", "bmp" -> IMAGE
                else -> OTHER
            }
        }

        fun fromMimeType(mimeType: String?): DocumentFileType {
            if (mimeType == null) return OTHER
            return when {
                mimeType.contains("pdf", ignoreCase = true) -> PDF
                mimeType.contains("word", ignoreCase = true) || mimeType.contains("msword", ignoreCase = true) || mimeType.contains("document", ignoreCase = true) -> WORD
                mimeType.contains("sheet", ignoreCase = true) || mimeType.contains("excel", ignoreCase = true) || mimeType.contains("csv", ignoreCase = true) -> EXCEL
                mimeType.contains("presentation", ignoreCase = true) || mimeType.contains("powerpoint", ignoreCase = true) -> POWERPOINT
                mimeType.startsWith("text/", ignoreCase = true) -> TEXT
                mimeType.startsWith("image/", ignoreCase = true) -> IMAGE
                else -> OTHER
            }
        }
    }
}
