package com.example.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.ui.components.CategoryBadge
import com.example.ui.components.DocuProTopBar
import com.example.ui.components.DocumentCard
import com.example.ui.components.FileOptionBottomSheet
import com.example.ui.components.QuickActionButton
import com.example.ui.theme.ColorExcelGreen
import com.example.ui.theme.ColorImagePurple
import com.example.ui.theme.ColorPdfRed
import com.example.ui.theme.ColorPptOrange
import com.example.ui.theme.ColorTextGray
import com.example.ui.theme.ColorWordBlue
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.PrimaryBlue600

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Security

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToNewDoc: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToImageToPdf: () -> Unit,
    onNavigateToPdfTools: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFilesCategory: (DocumentFileType) -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenDetails: (DocumentItem) -> Unit
) {
    val context = LocalContext.current
    val recentDocs by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val starredDocs by viewModel.starredDocuments.collectAsStateWithLifecycle()
    val allDocs by viewModel.allDocuments.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var selectedBottomSheetDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<DocumentFileType?>(null) }

    // Runtime Storage Permission handling
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        hasStoragePermission = granted
        if (granted) {
            viewModel.scanStorage()
        }
    }

    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            viewModel.scanStorage()
        }
    }

    // SAF Open File Launcher
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val doc = viewModel.importDocument(uri)
                onOpenDocument(doc)
            }
        }
    }

    Scaffold(
        topBar = {
            DocuProTopBar(
                onSearchClick = onNavigateToSearch,
                onProfileClick = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_lazy_column"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Storage Permission Card Banner if missing
            if (!hasStoragePermission) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Storage Permission",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Storage Permission Required",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Allow DocuPro to access document files on your device",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = {
                                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(
                                            android.Manifest.permission.READ_MEDIA_IMAGES
                                        )
                                    } else {
                                        arrayOf(
                                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        )
                                    }
                                    permissionLauncher.launch(permissions)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(text = "Grant", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            // Main Action Banner Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // New Document Button
                    Button(
                        onClick = onNavigateToNewDoc,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_new_document"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue600)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "New", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "New Doc", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }

                    // Scan Document Button
                    Button(
                        onClick = {
                            Toast.makeText(context, "Coming Soon: Document Scanner feature is coming soon!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_scan_document"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary)
                    ) {
                        Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "Scan", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Scan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }

                    // Open File Button
                    Button(
                        onClick = {
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
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_open_file"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Filled.FolderOpen, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Open File", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                }
            }

            // Modern Quick Action Area
            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                    Text(
                        text = "QUICK TOOLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            QuickActionButton(
                                title = "PDF",
                                icon = Icons.Outlined.PictureAsPdf,
                                brandColor = ColorPdfRed,
                                onClick = { onNavigateToFilesCategory(DocumentFileType.PDF) }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "Word",
                                icon = Icons.Outlined.Description,
                                brandColor = ColorWordBlue,
                                onClick = { onNavigateToFilesCategory(DocumentFileType.WORD) }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "Excel",
                                icon = Icons.Outlined.TableChart,
                                brandColor = ColorExcelGreen,
                                onClick = { onNavigateToFilesCategory(DocumentFileType.EXCEL) }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "PowerPoint",
                                icon = Icons.Outlined.Slideshow,
                                brandColor = ColorPptOrange,
                                onClick = { onNavigateToFilesCategory(DocumentFileType.POWERPOINT) }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "Merge PDFs",
                                icon = Icons.Filled.CallMerge,
                                brandColor = ColorPdfRed,
                                onClick = { onNavigateToPdfTools(0) }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "Split PDFs",
                                icon = Icons.Filled.CallSplit,
                                brandColor = ColorPdfRed,
                                onClick = { onNavigateToPdfTools(1) }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "Compress PDF",
                                icon = Icons.Filled.Compress,
                                brandColor = ColorPdfRed,
                                onClick = { onNavigateToPdfTools(2) }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "Reorder Pages",
                                icon = Icons.Filled.Reorder,
                                brandColor = ColorPdfRed,
                                onClick = { onNavigateToPdfTools(3) }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "Scan Doc",
                                icon = Icons.Outlined.QrCodeScanner,
                                brandColor = IndigoSecondary,
                                onClick = {
                                    Toast.makeText(context, "Coming Soon: Document Scanner feature is coming soon!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        item {
                            QuickActionButton(
                                title = "Image to PDF",
                                icon = Icons.Outlined.Transform,
                                brandColor = ColorImagePurple,
                                onClick = onNavigateToImageToPdf
                            )
                        }
                    }
                }
            }

            // Category Filter Badges
            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                    Text(
                        text = "CATEGORIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            CategoryBadge(
                                title = "All",
                                icon = Icons.Outlined.Description,
                                brandColor = PrimaryBlue600,
                                count = allDocs.size,
                                isSelected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null }
                            )
                        }
                        item {
                            CategoryBadge(
                                title = "PDFs",
                                icon = Icons.Outlined.PictureAsPdf,
                                brandColor = ColorPdfRed,
                                count = allDocs.count { it.fileType == DocumentFileType.PDF },
                                isSelected = selectedCategoryFilter == DocumentFileType.PDF,
                                onClick = { selectedCategoryFilter = DocumentFileType.PDF }
                            )
                        }
                        item {
                            CategoryBadge(
                                title = "Word",
                                icon = Icons.Outlined.Description,
                                brandColor = ColorWordBlue,
                                count = allDocs.count { it.fileType == DocumentFileType.WORD },
                                isSelected = selectedCategoryFilter == DocumentFileType.WORD,
                                onClick = { selectedCategoryFilter = DocumentFileType.WORD }
                            )
                        }
                        item {
                            CategoryBadge(
                                title = "Excel",
                                icon = Icons.Outlined.TableChart,
                                brandColor = ColorExcelGreen,
                                count = allDocs.count { it.fileType == DocumentFileType.EXCEL },
                                isSelected = selectedCategoryFilter == DocumentFileType.EXCEL,
                                onClick = { selectedCategoryFilter = DocumentFileType.EXCEL }
                            )
                        }
                        item {
                            CategoryBadge(
                                title = "PowerPoint",
                                icon = Icons.Outlined.Slideshow,
                                brandColor = ColorPptOrange,
                                count = allDocs.count { it.fileType == DocumentFileType.POWERPOINT },
                                isSelected = selectedCategoryFilter == DocumentFileType.POWERPOINT,
                                onClick = { selectedCategoryFilter = DocumentFileType.POWERPOINT }
                            )
                        }
                        item {
                            CategoryBadge(
                                title = "Text",
                                icon = Icons.Outlined.Description,
                                brandColor = ColorTextGray,
                                count = allDocs.count { it.fileType == DocumentFileType.TEXT },
                                isSelected = selectedCategoryFilter == DocumentFileType.TEXT,
                                onClick = { selectedCategoryFilter = DocumentFileType.TEXT }
                            )
                        }
                    }
                }
            }

            // Starred Documents Header
            if (starredDocs.isNotEmpty() && selectedCategoryFilter == null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STARRED DOCUMENTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
                items(starredDocs.take(3)) { doc ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        DocumentCard(
                            document = doc,
                            onClick = { onOpenDocument(doc) },
                            onStarToggle = { viewModel.toggleStar(doc) },
                            onOptionsClick = { selectedBottomSheetDoc = doc }
                        )
                    }
                }
            }

            // Recent Documents Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategoryFilter != null) "${selectedCategoryFilter!!.categoryName.uppercase()} DOCUMENTS" else "RECENT DOCUMENTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            val displayDocs = if (selectedCategoryFilter != null) {
                allDocs.filter { it.fileType == selectedCategoryFilter }
            } else {
                recentDocs
            }

            if (displayDocs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue600.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = "Empty",
                                    tint = PrimaryBlue600,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No documents found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ New Doc' or '+ Scan' to create your first document.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayDocs) { doc ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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

    // Options Bottom Sheet
    FileOptionBottomSheet(
        document = selectedBottomSheetDoc,
        onDismiss = { selectedBottomSheetDoc = null },
        onOpen = { doc -> onOpenDocument(doc) },
        onDetails = { doc -> onOpenDetails(doc) },
        onShare = { /* Share intent */ },
        onStar = { doc -> viewModel.toggleStar(doc) },
        onRename = { /* Rename */ },
        onCopy = { /* Copy */ },
        onMove = { /* Move */ },
        onDelete = { doc -> viewModel.moveToTrash(doc) },
        onPrint = { /* Print */ }
    )
}
