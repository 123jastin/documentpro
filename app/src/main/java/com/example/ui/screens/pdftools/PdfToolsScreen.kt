package com.example.ui.screens.pdftools

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DocuProDatabase
import com.example.data.engine.PdfDocumentEngine
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.data.repository.DocumentRepository
import com.example.ui.theme.ColorPdfRed
import com.example.ui.theme.PrimaryBlue600
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    initialToolIndex: Int = 0,
    onBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedToolIndex by remember { mutableIntStateOf(initialToolIndex) }
    val pdfEngine = remember { PdfDocumentEngine() }

    val db = remember { DocuProDatabase.getDatabase(context) }
    val repository = remember {
        DocumentRepository(context, db.documentDao(), db.annotationDao(), db.scanDao())
    }

    var isLoading by remember { mutableStateOf(false) }

    // Merge PDF state
    val mergeUris = remember { mutableStateListOf<Uri>() }

    // Split PDF state
    var splitUri by remember { mutableStateOf<Uri?>(null) }
    var splitPagesInput by remember { mutableStateOf("1-2") }

    // Compress PDF state
    var compressUri by remember { mutableStateOf<Uri?>(null) }
    var compressLevel by remember { mutableStateOf("Medium (40% smaller)") }

    // Reorder Pages state
    var reorderUri by remember { mutableStateOf<Uri?>(null) }
    val pageThumbnails = remember { mutableStateListOf<android.graphics.Bitmap>() }
    val pageOrder = remember { mutableStateListOf<Int>() }

    val mergePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        mergeUris.addAll(uris)
    }

    val singlePdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            when (selectedToolIndex) {
                1 -> splitUri = uri
                2 -> compressUri = uri
                3 -> {
                    reorderUri = uri
                    scope.launch {
                        isLoading = true
                        pageThumbnails.clear()
                        pageOrder.clear()
                        val result = pdfEngine.parseDocument(context, uri)
                        if (result is com.example.data.model.DocumentResult.Success) {
                            val count = result.data.pageCount
                            for (i in 0 until count) {
                                val bmp = pdfEngine.renderPageBitmap(context, uri, i, targetWidth = 360)
                                if (bmp != null) {
                                    pageThumbnails.add(bmp)
                                    pageOrder.add(i)
                                }
                            }
                        }
                        isLoading = false
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text(text = "PDF Professional Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                .testTag("pdf_tools_screen")
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedToolIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedToolIndex == 0,
                    onClick = { selectedToolIndex = 0 },
                    text = { Text("Merge", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(imageVector = Icons.Filled.CallMerge, contentDescription = "Merge") }
                )
                Tab(
                    selected = selectedToolIndex == 1,
                    onClick = { selectedToolIndex = 1 },
                    text = { Text("Split", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(imageVector = Icons.Filled.CallSplit, contentDescription = "Split") }
                )
                Tab(
                    selected = selectedToolIndex == 2,
                    onClick = { selectedToolIndex = 2 },
                    text = { Text("Compress", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(imageVector = Icons.Filled.Compress, contentDescription = "Compress") }
                )
                Tab(
                    selected = selectedToolIndex == 3,
                    onClick = { selectedToolIndex = 3 },
                    text = { Text("Reorder", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(imageVector = Icons.Filled.Reorder, contentDescription = "Reorder") }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ColorPdfRed)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Processing PDF...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            } else {
                when (selectedToolIndex) {
                    // TAB 0: MERGE PDFs
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Combine multiple PDF files into one single document.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item {
                                Button(
                                    onClick = { mergePickerLauncher.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue600)
                                ) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add PDFs")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Select PDF Files to Merge", fontWeight = FontWeight.Bold)
                                }
                            }
                            itemsIndexed(mergeUris) { idx, uri ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = "PDF", tint = ColorPdfRed)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = uri.lastPathSegment?.substringAfterLast('/') ?: "PDF #${idx + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { mergeUris.removeAt(idx) }) {
                                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                            if (mergeUris.size >= 2) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                val pdfDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
                                                val outFile = File(pdfDir, "Merged_PDF_${System.currentTimeMillis() % 10000}.pdf")
                                                val success = pdfEngine.mergePdfs(context, mergeUris, outFile)
                                                isLoading = false
                                                if (success) {
                                                    val doc = DocumentItem(
                                                        uriString = Uri.fromFile(outFile).toString(),
                                                        displayName = outFile.name,
                                                        extension = "pdf",
                                                        fileType = DocumentFileType.PDF,
                                                        sizeBytes = outFile.length(),
                                                        dateModified = System.currentTimeMillis(),
                                                        filePath = outFile.absolutePath
                                                    )
                                                    repository.insertDocument(doc)
                                                    Toast.makeText(context, "PDF Merged Successfully!", Toast.LENGTH_SHORT).show()
                                                    onOpenDocument(doc)
                                                } else {
                                                    Toast.makeText(context, "Failed to merge PDFs", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorPdfRed)
                                    ) {
                                        Icon(imageVector = Icons.Filled.CallMerge, contentDescription = "Merge")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Merge ${mergeUris.size} PDFs Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: SPLIT PDF
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Extract page ranges from a PDF document into a new file.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { singlePdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue600)
                            ) {
                                Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = "Select PDF")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (splitUri != null) "Change PDF File" else "Select PDF to Split", fontWeight = FontWeight.Bold)
                            }

                            if (splitUri != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Selected", tint = PrimaryBlue600)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = splitUri?.lastPathSegment?.substringAfterLast('/') ?: "Selected PDF", fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                OutlinedTextField(
                                    value = splitPagesInput,
                                    onValueChange = { splitPagesInput = it },
                                    label = { Text("Page Range (e.g., 1-2 or 1,3,5)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            val targetPages = mutableListOf<Int>()
                                            try {
                                                splitPagesInput.split(",").forEach { part ->
                                                    val trimmed = part.trim()
                                                    if (trimmed.contains("-")) {
                                                        val start = trimmed.substringBefore("-").toInt() - 1
                                                        val end = trimmed.substringAfter("-").toInt() - 1
                                                        for (p in start..end) targetPages.add(p)
                                                    } else if (trimmed.isNotEmpty()) {
                                                        targetPages.add(trimmed.toInt() - 1)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Invalid page format", Toast.LENGTH_SHORT).show()
                                                isLoading = false
                                                return@launch
                                            }

                                            val pdfDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
                                            val outFile = File(pdfDir, "Split_PDF_${System.currentTimeMillis() % 10000}.pdf")
                                            val success = pdfEngine.splitPdf(context, splitUri!!, targetPages, outFile)
                                            isLoading = false
                                            if (success) {
                                                val doc = DocumentItem(
                                                    uriString = Uri.fromFile(outFile).toString(),
                                                    displayName = outFile.name,
                                                    extension = "pdf",
                                                    fileType = DocumentFileType.PDF,
                                                    sizeBytes = outFile.length(),
                                                    dateModified = System.currentTimeMillis(),
                                                    filePath = outFile.absolutePath
                                                )
                                                repository.insertDocument(doc)
                                                Toast.makeText(context, "PDF Split Successfully!", Toast.LENGTH_SHORT).show()
                                                onOpenDocument(doc)
                                            } else {
                                                Toast.makeText(context, "Failed to split PDF", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorPdfRed)
                                ) {
                                    Icon(imageVector = Icons.Filled.CallSplit, contentDescription = "Split")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Extract Selected Pages", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // TAB 2: COMPRESS PDF
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Reduce PDF file size for fast emailing and sharing.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { singlePdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue600)
                            ) {
                                Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = "Select PDF")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (compressUri != null) "Change PDF File" else "Select PDF to Compress", fontWeight = FontWeight.Bold)
                            }

                            if (compressUri != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Selected", tint = PrimaryBlue600)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = compressUri?.lastPathSegment?.substringAfterLast('/') ?: "Selected PDF", fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            val pdfDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
                                            val outFile = File(pdfDir, "Compressed_${System.currentTimeMillis() % 10000}.pdf")
                                            val success = pdfEngine.compressPdf(context, compressUri!!, outFile, targetWidthScale = 0.65f)
                                            isLoading = false
                                            if (success) {
                                                val doc = DocumentItem(
                                                    uriString = Uri.fromFile(outFile).toString(),
                                                    displayName = outFile.name,
                                                    extension = "pdf",
                                                    fileType = DocumentFileType.PDF,
                                                    sizeBytes = outFile.length(),
                                                    dateModified = System.currentTimeMillis(),
                                                    filePath = outFile.absolutePath
                                                )
                                                repository.insertDocument(doc)
                                                Toast.makeText(context, "PDF Compressed Successfully!", Toast.LENGTH_SHORT).show()
                                                onOpenDocument(doc)
                                            } else {
                                                Toast.makeText(context, "Failed to compress PDF", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorPdfRed)
                                ) {
                                    Icon(imageVector = Icons.Filled.Compress, contentDescription = "Compress")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Compress PDF Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // TAB 3: REORDER PAGES
                    3 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        ) {
                            Text(
                                text = "Rearrange page order or remove pages from your PDF.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { singlePdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue600)
                            ) {
                                Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = "Select PDF")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (reorderUri != null) "Change PDF File" else "Select PDF to Reorder Pages", fontWeight = FontWeight.Bold)
                            }

                            if (pageThumbnails.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(pageOrder) { gridIndex, originalPageIndex ->
                                        val bmp = pageThumbnails.getOrNull(originalPageIndex)
                                        Card(
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                if (bmp != null) {
                                                    Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = "Page ${gridIndex + 1}",
                                                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp))
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(text = "Page ${gridIndex + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (gridIndex > 0) {
                                                                val temp = pageOrder[gridIndex]
                                                                pageOrder[gridIndex] = pageOrder[gridIndex - 1]
                                                                pageOrder[gridIndex - 1] = temp
                                                            }
                                                        },
                                                        enabled = gridIndex > 0
                                                    ) {
                                                        Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = "Move Up")
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (gridIndex < pageOrder.size - 1) {
                                                                val temp = pageOrder[gridIndex]
                                                                pageOrder[gridIndex] = pageOrder[gridIndex + 1]
                                                                pageOrder[gridIndex + 1] = temp
                                                            }
                                                        },
                                                        enabled = gridIndex < pageOrder.size - 1
                                                    ) {
                                                        Icon(imageVector = Icons.Filled.ArrowDownward, contentDescription = "Move Down")
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (pageOrder.size > 1) {
                                                                pageOrder.removeAt(gridIndex)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Page", tint = Color.Red)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            val pdfDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
                                            val outFile = File(pdfDir, "Reordered_${System.currentTimeMillis() % 10000}.pdf")
                                            val success = pdfEngine.reorderPdfPages(context, reorderUri!!, pageOrder, outFile)
                                            isLoading = false
                                            if (success) {
                                                val doc = DocumentItem(
                                                    uriString = Uri.fromFile(outFile).toString(),
                                                    displayName = outFile.name,
                                                    extension = "pdf",
                                                    fileType = DocumentFileType.PDF,
                                                    sizeBytes = outFile.length(),
                                                    dateModified = System.currentTimeMillis(),
                                                    filePath = outFile.absolutePath
                                                )
                                                repository.insertDocument(doc)
                                                Toast.makeText(context, "Pages Reordered Successfully!", Toast.LENGTH_SHORT).show()
                                                onOpenDocument(doc)
                                            } else {
                                                Toast.makeText(context, "Failed to reorder PDF pages", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorPdfRed)
                                ) {
                                    Icon(imageVector = Icons.Filled.Reorder, contentDescription = "Save Reordered PDF")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Save Reordered PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
