package com.example.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DocumentItem
import com.example.ui.screens.files.FileManagerViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.components.FileOptionBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: FileManagerViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenDetails: (DocumentItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    var selectedBottomSheetDoc by remember { mutableStateOf<DocumentItem?>(null) }

    val filtered = remember(query, documents) {
        if (query.isBlank()) emptyList()
        else documents.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                    it.contentSummary.contains(query, ignoreCase = true) ||
                    it.extension.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search document name or text...") },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .testTag("search_input_field")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(filtered, key = { it.uriString }) { doc ->
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        DocumentCard(
                            document = doc,
                            onClick = { onOpenDocument(doc) },
                            onStarToggle = { viewModel.toggleStar(doc) },
                            onOptionsClick = { selectedBottomSheetDoc = doc }
                        )
                    }
                }
            }
        }
    }

    val scope = rememberCoroutineScope()

    FileOptionBottomSheet(
        document = selectedBottomSheetDoc,
        onDismiss = { selectedBottomSheetDoc = null },
        onOpen = { doc -> onOpenDocument(doc) },
        onDetails = { doc -> onOpenDetails(doc) },
        onShare = { /* Share */ },
        onStar = { doc -> viewModel.toggleStar(doc) },
        onRename = { /* Rename */ },
        onCopy = { /* Copy */ },
        onMove = { /* Move */ },
        onDelete = { doc ->
            scope.launch {
                viewModel.repository.moveToTrash(doc.uriString)
            }
        },
        onPrint = { /* Print */ }
    )
}
