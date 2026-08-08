package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE isTrash = 0 ORDER BY lastOpenedTime DESC, dateModified DESC")
    fun getAllDocuments(): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents WHERE isRecent = 1 AND isTrash = 0 ORDER BY lastOpenedTime DESC LIMIT 20")
    fun getRecentDocuments(): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents WHERE isStarred = 1 AND isTrash = 0 ORDER BY dateModified DESC")
    fun getStarredDocuments(): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents WHERE fileType = :type AND isTrash = 0 ORDER BY dateModified DESC")
    fun getDocumentsByType(type: DocumentFileType): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents WHERE isTrash = 1 ORDER BY dateModified DESC")
    fun getTrashDocuments(): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents WHERE uriString = :uri LIMIT 1")
    suspend fun getDocumentByUri(uri: String): DocumentItem?

    @Query("SELECT * FROM documents WHERE (displayName LIKE '%' || :query || '%' OR contentSummary LIKE '%' || :query || '%') AND isTrash = 0 ORDER BY dateModified DESC")
    fun searchDocuments(query: String): Flow<List<DocumentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentItem>)

    @Update
    suspend fun updateDocument(document: DocumentItem)

    @Query("UPDATE documents SET isStarred = :isStarred WHERE uriString = :uri")
    suspend fun setStarred(uri: String, isStarred: Boolean)

    @Query("UPDATE documents SET isRecent = 1, lastOpenedTime = :openedTime, lastPageRead = :lastPage WHERE uriString = :uri")
    suspend fun updateReadingProgress(uri: String, openedTime: Long, lastPage: Int)

    @Query("UPDATE documents SET displayName = :newName WHERE uriString = :uri")
    suspend fun renameDocument(uri: String, newName: String)

    @Query("UPDATE documents SET isTrash = 1 WHERE uriString = :uri")
    suspend fun moveToTrash(uri: String)

    @Query("DELETE FROM documents WHERE uriString = :uri")
    suspend fun deleteDocumentPermanently(uri: String)

    @Query("DELETE FROM documents WHERE isTrash = 1")
    suspend fun emptyTrash()
}
