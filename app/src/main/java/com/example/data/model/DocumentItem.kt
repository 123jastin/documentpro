package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentItem(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val extension: String,
    val fileType: DocumentFileType,
    val sizeBytes: Long,
    val dateModified: Long,
    val dateCreated: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false,
    val isRecent: Boolean = false,
    val lastOpenedTime: Long = 0L,
    val lastPageRead: Int = 0,
    val pageCount: Int = 1,
    val folderPath: String = "Documents",
    val isTrash: Boolean = false,
    val contentSummary: String = "",
    val filePath: String = ""
)
