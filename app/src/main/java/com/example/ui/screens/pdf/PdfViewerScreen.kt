package com.example.ui.screens.pdf

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DocuProDatabase
import com.example.data.engine.PdfDocumentEngine
import com.example.data.model.AnnotationType
import com.example.data.model.PdfAnnotation
import com.example.data.repository.DocumentRepository
import com.example.ui.theme.ColorPdfRed
import com.example.ui.theme.PrimaryBlue600
import com.example.ui.theme.StarGold
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    documentUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfEngine = remember { PdfDocumentEngine() }
    val db = remember { DocuProDatabase.getDatabase(context) }
    val repository = remember { DocumentRepository(context, db.documentDao(), db.annotationDao(), db.scanDao()) }

    var pageCount by remember { mutableIntStateOf(1) }
    var pageBitmaps by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var isAnnotationMode by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf(AnnotationType.PEN) }
    var activeColorHex by remember { mutableStateOf("#EF4444") }
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var isBookmarked by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val uri = remember(documentUri) { Uri.parse(documentUri) }

    // Persistent Annotations List from Room
    var localAnnotations by remember { mutableStateOf<List<PdfAnnotation>>(emptyList()) }

    LaunchedEffect(documentUri) {
        val result = pdfEngine.parseDocument(context, uri)
        if (result is com.example.data.model.DocumentResult.Success) {
            pageCount = result.data.pageCount
        }

        // Load annotations
        val annotationsFlow = repository.getAnnotationsForDocument(documentUri)
        localAnnotations = annotationsFlow.first()

        // Render page bitmaps asynchronously
        val map = mutableMapOf<Int, Bitmap>()
        for (i in 0 until pageCount.coerceAtMost(10)) {
            val bmp = pdfEngine.renderPageBitmap(context, uri, i)
            if (bmp != null) map[i] = bmp
        }
        pageBitmaps = map
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search text in PDF...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    } else {
                        Column {
                            Text(
                                text = uri.lastPathSegment?.substringAfterLast('/') ?: "PDF Document",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Page ${listState.firstVisibleItemIndex + 1} of $pageCount",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { isBookmarked = !isBookmarked }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) StarGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isAnnotationMode = !isAnnotationMode }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Annotate",
                            tint = if (isAnnotationMode) PrimaryBlue600 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (isAnnotationMode) {
                // Annotation Controls Toolbar
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tool Selectors
                        IconButton(onClick = { selectedTool = AnnotationType.PEN }) {
                            Icon(
                                imageVector = Icons.Filled.Brush,
                                contentDescription = "Pen",
                                tint = if (selectedTool == AnnotationType.PEN) ColorPdfRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { selectedTool = AnnotationType.HIGHLIGHT }) {
                            Icon(
                                imageVector = Icons.Outlined.Highlight,
                                contentDescription = "Highlight",
                                tint = if (selectedTool == AnnotationType.HIGHLIGHT) StarGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { selectedTool = AnnotationType.STICKY_NOTE }) {
                            Icon(
                                imageVector = Icons.Outlined.NoteAdd,
                                contentDescription = "Note",
                                tint = if (selectedTool == AnnotationType.STICKY_NOTE) PrimaryBlue600 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { selectedTool = AnnotationType.SHAPE_RECT }) {
                            Icon(
                                imageVector = Icons.Outlined.CropSquare,
                                contentDescription = "Shape",
                                tint = if (selectedTool == AnnotationType.SHAPE_RECT) PrimaryBlue600 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Color picker quick dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("#EF4444", "#3B82F6", "#10B981", "#F59E0B").forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { activeColorHex = hex }
                                )
                            }
                        }
                    }
                }
            } else {
                // Standard Reader Bottom Bar
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) }) {
                            Icon(imageVector = Icons.Outlined.ZoomOut, contentDescription = "Zoom Out")
                        }
                        Text(
                            text = "${(zoomScale * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) }) {
                            Icon(imageVector = Icons.Outlined.ZoomIn, contentDescription = "Zoom In")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE2E8F0))
                .testTag("pdf_viewer_canvas_container")
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(pageCount) { pageIdx ->
                    val bmp = pageBitmaps[pageIdx]
                    Card(
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        if (bmp != null) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "PDF Page ${pageIdx + 1}",
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Annotation Canvas Layer
                                var currentPath by remember { mutableStateOf(Path()) }
                                val activeColor = Color(android.graphics.Color.parseColor(activeColorHex))

                                Canvas(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .pointerInput(isAnnotationMode) {
                                            if (isAnnotationMode) {
                                                detectTransformGestures { _, pan, _, _ ->
                                                    if (currentPath.isEmpty) {
                                                        currentPath.moveTo(pan.x, pan.y)
                                                    } else {
                                                        currentPath.lineTo(pan.x + pan.x, pan.y + pan.y)
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    drawPath(
                                        path = currentPath,
                                        color = activeColor,
                                        style = Stroke(width = strokeWidth)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryBlue600)
                            }
                        }
                    }
                }
            }
        }
    }
}
