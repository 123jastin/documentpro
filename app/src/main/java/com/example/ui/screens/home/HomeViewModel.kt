package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.DocuProDatabase
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DocuProDatabase.getDatabase(application)
    val repository = DocumentRepository(
        application,
        db.documentDao(),
        db.annotationDao(),
        db.scanDao()
    )

    init {
        viewModelScope.launch {
            repository.ensureSampleDocumentsExist()
        }
    }

    val recentDocuments: StateFlow<List<DocumentItem>> = repository.recentDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val starredDocuments: StateFlow<List<DocumentItem>> = repository.starredDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDocuments: StateFlow<List<DocumentItem>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleStar(document: DocumentItem) {
        viewModelScope.launch {
            repository.toggleStar(document.uriString, document.isStarred)
        }
    }

    fun moveToTrash(document: DocumentItem) {
        viewModelScope.launch {
            repository.moveToTrash(document.uriString)
        }
    }
}
