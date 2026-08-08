package com.example.data.engine

import android.content.Context
import android.net.Uri
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentResult

interface DocumentEngine {
    fun canHandle(fileType: DocumentFileType): Boolean
    suspend fun parseDocument(context: Context, uri: Uri): DocumentResult<ParsedDocumentContent>
}

data class ParsedDocumentContent(
    val title: String,
    val fileType: DocumentFileType,
    val pageCount: Int = 1,
    val textContent: String = "",
    val isEditable: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val spreadsheetData: SpreadsheetModel? = null,
    val presentationData: PresentationModel? = null
)

data class SpreadsheetModel(
    val sheets: List<SheetData> = emptyList()
)

data class SheetData(
    val name: String,
    val rowCount: Int,
    val columnCount: Int,
    val cells: Map<Pair<Int, Int>, CellData> // (row, col) -> Cell
)

data class CellData(
    val value: String,
    val formula: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val align: String = "LEFT"
)

data class PresentationModel(
    val slides: List<SlideData> = emptyList()
)

data class SlideData(
    val slideNumber: Int,
    val title: String,
    val bodyText: String,
    val notes: String = ""
)
