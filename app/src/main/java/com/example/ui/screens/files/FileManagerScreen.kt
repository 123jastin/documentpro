package com.example.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.ui.components.CategoryBadge
import com.example.ui.components.DocumentCard
import com.example.ui.components.FileOptionBottomSheet
import com.example.ui.theme.ColorExcelGreen
import com.example.ui.theme.ColorPdfRed
import com.example.ui.theme.ColorPptOrange
import com.example.ui.theme.ColorTextGray
import com.example.ui.theme.ColorWordBlue
import com.example.ui.theme.PrimaryBlue600
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.ui.platform.LocalContext
import com.example.ads.NativeDocumentAdCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel = viewModel(),
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenDetails: (DocumentItem) -> Unit,
    initialCategory: DocumentFileType? = null
) {
    val isGrid by viewModel.isGridView.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilterType.collectAsStateWithLifecycle()
    val sortMode by viewModel.currentSortMode.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedUris by viewModel.selectedUris.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedBottomSheetDoc by remember { mutableStateOf<DocumentItem?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val doc = viewModel.repository.importDocumentFromUri(uri)
                if (doc != null) {
                    onOpenDocument(doc)
                }
            }
        }
    }

    remember {
        if (initialCategory != null) {
            viewModel.setFilter(initialCategory)
        }
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = {
                    Text(
                        text = if (isMultiSelect) "${selectedUris.size} Selected" else "File Manager",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    if (isMultiSelect) {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = {
                            openFileLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "text/plain",
                                    "image/*"
                                )
                            )
                        }) {
                            Icon(imageVector = Icons.Filled.FolderOpen, contentDescription = "Open Phone Files", tint = PrimaryBlue600)
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Sort by Name") },
                                onClick = { viewModel.setSort(SortMode.NAME); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Date Modified") },
                                onClick = { viewModel.setSort(SortMode.DATE_MODIFIED); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by File Size") },
                                onClick = { viewModel.setSort(SortMode.SIZE); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by File Type") },
                                onClick = { viewModel.setSort(SortMode.FILE_TYPE); showSortMenu = false }
                            )
                        }

                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (isGrid) Icons.Filled.List else Icons.Filled.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("file_manager_screen")
        ) {
            // Filter Chips Bar
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryBadge(
                        title = "All Files",
                        icon = Icons.Outlined.Description,
                        brandColor = PrimaryBlue600,
                        isSelected = selectedFilter == null,
                        onClick = { viewModel.setFilter(null) }
                    )
                }
                item {
                    CategoryBadge(
                        title = "PDFs",
                        icon = Icons.Outlined.PictureAsPdf,
                        brandColor = ColorPdfRed,
                        isSelected = selectedFilter == DocumentFileType.PDF,
                        onClick = { viewModel.setFilter(DocumentFileType.PDF) }
                    )
                }
                item {
                    CategoryBadge(
                        title = "Word",
                        icon = Icons.Outlined.Description,
                        brandColor = ColorWordBlue,
                        isSelected = selectedFilter == DocumentFileType.WORD,
                        onClick = { viewModel.setFilter(DocumentFileType.WORD) }
                    )
                }
                item {
                    CategoryBadge(
                        title = "Excel",
                        icon = Icons.Outlined.TableChart,
                        brandColor = ColorExcelGreen,
                        isSelected = selectedFilter == DocumentFileType.EXCEL,
                        onClick = { viewModel.setFilter(DocumentFileType.EXCEL) }
                    )
                }
                item {
                    CategoryBadge(
                        title = "PowerPoint",
                        icon = Icons.Outlined.Slideshow,
                        brandColor = ColorPptOrange,
                        isSelected = selectedFilter == DocumentFileType.POWERPOINT,
                        onClick = { viewModel.setFilter(DocumentFileType.POWERPOINT) }
                    )
                }
                item {
                    CategoryBadge(
                        title = "Text",
                        icon = Icons.Outlined.Description,
                        brandColor = ColorTextGray,
                        isSelected = selectedFilter == DocumentFileType.TEXT,
                        onClick = { viewModel.setFilter(DocumentFileType.TEXT) }
                    )
                }
            }

            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue600.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Empty",
                                tint = PrimaryBlue600,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No files match current filter",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select another category or import files into DocuPro.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val adPositions = remember(documents.size) {
                    when {
                        documents.size >= 20 -> setOf(4, 11, 18)
                        documents.size >= 10 -> setOf(4, 9)
                        documents.size >= 5 -> setOf(4)
                        else -> emptySet()
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isGrid) 2 else 1),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(documents, key = { _, doc -> doc.uriString }) { index, doc ->
                        val isSelected = selectedUris.contains(doc.uriString)

                        Box(modifier = Modifier.fillMaxWidth()) {
                            DocumentCard(
                                document = doc,
                                isGridView = isGrid,
                                onClick = {
                                    if (isMultiSelect) {
                                        viewModel.toggleSelection(doc.uriString)
                                    } else {
                                        onOpenDocument(doc)
                                    }
                                },
                                onStarToggle = { viewModel.toggleStar(doc) },
                                onOptionsClick = { selectedBottomSheetDoc = doc }
                            )

                            if (isMultiSelect) {
                                IconButton(
                                    onClick = { viewModel.toggleSelection(doc.uriString) },
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = "Select",
                                        tint = if (isSelected) PrimaryBlue600 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Native Ad according to AdMob Category Rules
                        if (adPositions.contains(index)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                NativeDocumentAdCard()
                            }
                        }
                    }
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
        onDelete = { doc ->
            scope.launch {
                viewModel.repository.moveToTrash(doc.uriString)
            }
        },
        onPrint = { /* Print */ }
    )
}
