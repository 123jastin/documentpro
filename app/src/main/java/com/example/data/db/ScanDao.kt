package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ScanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scan_pages WHERE sessionId = :sessionId ORDER BY pageNumber ASC")
    fun getScanPages(sessionId: String): Flow<List<ScanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanPage(page: ScanItem): Long

    @Query("DELETE FROM scan_pages WHERE sessionId = :sessionId")
    suspend fun clearScanSession(sessionId: String)

    @Delete
    suspend fun deleteScanPage(page: ScanItem)
}
