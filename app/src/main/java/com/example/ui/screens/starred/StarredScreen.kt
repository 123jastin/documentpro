package com.example.ui.screens.starred

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DocumentItem
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.components.FileOptionBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredScreen(
    viewModel: HomeViewModel = viewModel(),
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenDetails: (DocumentItem) -> Unit
) {
    val starred by viewModel.starredDocuments.collectAsStateWithLifecycle()
    var selectedBottomSheetDoc by remember { mutableStateOf<DocumentItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text(text = "Starred Files", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("starred_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            items(starred, key = { it.uriString }) { doc ->
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
        onDelete = { doc -> viewModel.moveToTrash(doc) },
        onPrint = { /* Print */ }
    )
}
