package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_pages")
data class ScanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val imagePath: String,
    val pageNumber: Int,
    val ocrText: String = "",
    val filterMode: String = "ORIGINAL", // ORIGINAL, GRAYSCALE, BLACK_WHITE, ENHANCE
    val createdTime: Long = System.currentTimeMillis()
)
