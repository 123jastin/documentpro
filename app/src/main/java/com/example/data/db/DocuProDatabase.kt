package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.DocumentFolder
import com.example.data.model.DocumentItem
import com.example.data.model.PdfAnnotation
import com.example.data.model.ScanItem

@Database(
    entities = [
        DocumentItem::class,
        DocumentFolder::class,
        PdfAnnotation::class,
        ScanItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DocuProDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: DocuProDatabase? = null

        fun getDatabase(context: Context): DocuProDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocuProDatabase::class.java,
                    "docupro_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
