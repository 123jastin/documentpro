package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AnnotationType {
    HIGHLIGHT,
    UNDERLINE,
    STRIKETHROUGH,
    PEN,
    ERASER,
    TEXT_BOX,
    STICKY_NOTE,
    SHAPE_RECT,
    SHAPE_CIRCLE,
    SHAPE_ARROW
}

@Entity(tableName = "pdf_annotations")
data class PdfAnnotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentUri: String,
    val pageIndex: Int,
    val type: AnnotationType,
    val colorHex: String,
    val strokeWidth: Float = 4f,
    val pathData: String = "", // SVG path or point series for freehand
    val textContent: String = "",
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
