package com.example.ui.screens.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.DocuProDatabase
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode {
    NAME,
    DATE_MODIFIED,
    SIZE,
    FILE_TYPE
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DocuProDatabase.getDatabase(application)
    val repository = DocumentRepository(
        application,
        db.documentDao(),
        db.annotationDao(),
        db.scanDao()
    )

    val isGridView = MutableStateFlow(false)
    val selectedFilterType = MutableStateFlow<DocumentFileType?>(null)
    val currentSortMode = MutableStateFlow(SortMode.DATE_MODIFIED)
    val searchQuery = MutableStateFlow("")
    val selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val isMultiSelectMode = MutableStateFlow(false)

    val allDocuments: StateFlow<List<DocumentItem>> = combine(
        repository.allDocuments,
        selectedFilterType,
        currentSortMode,
        searchQuery
    ) { docs, filter, sort, query ->
        var list = docs.filter { !it.isTrash }

        if (filter != null) {
            list = list.filter { it.fileType == filter }
        }

        if (query.isNotBlank()) {
            list = list.filter { it.displayName.contains(query, ignoreCase = true) }
        }

        when (sort) {
            SortMode.NAME -> list.sortedBy { it.displayName.lowercase() }
            SortMode.DATE_MODIFIED -> list.sortedByDescending { it.dateModified }
            SortMode.SIZE -> list.sortedByDescending { it.sizeBytes }
            SortMode.FILE_TYPE -> list.sortedBy { it.fileType.name }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleViewMode() {
        isGridView.value = !isGridView.value
    }

    fun setFilter(type: DocumentFileType?) {
        selectedFilterType.value = type
    }

    fun setSort(sort: SortMode) {
        currentSortMode.value = sort
    }

    fun toggleStar(document: DocumentItem) {
        viewModelScope.launch {
            repository.toggleStar(document.uriString, document.isStarred)
        }
    }

    fun toggleSelection(uri: String) {
        val current = selectedUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        selectedUris.value = current
        isMultiSelectMode.value = current.isNotEmpty()
    }

    fun clearSelection() {
        selectedUris.value = emptySet()
        isMultiSelectMode.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            selectedUris.value.forEach { uri ->
                repository.moveToTrash(uri)
            }
            clearSelection()
        }
    }
}
