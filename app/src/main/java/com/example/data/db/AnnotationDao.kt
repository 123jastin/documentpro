package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PdfAnnotation
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {

    @Query("SELECT * FROM pdf_annotations WHERE documentUri = :uri ORDER BY pageIndex ASC, id ASC")
    fun getAnnotationsForDocument(uri: String): Flow<List<PdfAnnotation>>

    @Query("SELECT * FROM pdf_annotations WHERE documentUri = :uri AND pageIndex = :pageIndex ORDER BY id ASC")
    suspend fun getAnnotationsForPage(uri: String, pageIndex: Int): List<PdfAnnotation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: PdfAnnotation): Long

    @Delete
    suspend fun deleteAnnotation(annotation: PdfAnnotation)

    @Query("DELETE FROM pdf_annotations WHERE documentUri = :uri")
    suspend fun clearAnnotationsForDocument(uri: String)
}
