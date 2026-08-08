package com.example.ui.screens.text

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.db.DocuProDatabase
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.data.repository.DocumentRepository
import com.example.data.engine.TextDocumentEngine
import com.example.ui.components.SaveAsDialog
import com.example.ui.theme.ColorTextGray
import com.example.ui.theme.PrimaryBlue600
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    documentUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { TextDocumentEngine() }
    val db = remember { DocuProDatabase.getDatabase(context) }
    val repository = remember { DocumentRepository(context, db.documentDao(), db.annotationDao(), db.scanDao()) }
    val uri = remember(documentUri) { Uri.parse(documentUri) }

    var textContent by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("Note.txt") }
    var isFullScreen by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(documentUri) {
        val result = engine.parseDocument(context, uri)
        if (result is com.example.data.model.DocumentResult.Success) {
            textContent = result.data.textContent
            fileName = result.data.title
        }
    }

    val lines = textContent.lines()
    val wordCount = if (textContent.isBlank()) 0 else textContent.split("\\s+".toRegex()).size

    if (showSaveAsDialog) {
        SaveAsDialog(
            initialName = fileName,
            onDismiss = { showSaveAsDialog = false },
            onSave = { newName ->
                showSaveAsDialog = false
                scope.launch {
                    val docsDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
                    val targetFile = File(docsDir, newName)
                    val targetUri = Uri.fromFile(targetFile)

                    val saveOk = engine.saveTextToUri(context, targetUri, textContent)
                    if (saveOk) {
                        fileName = newName
                        val doc = DocumentItem(
                            uriString = targetUri.toString(),
                            displayName = newName,
                            extension = "txt",
                            fileType = DocumentFileType.TEXT,
                            sizeBytes = targetFile.length().coerceAtLeast(100L),
                            dateModified = System.currentTimeMillis(),
                            filePath = targetFile.absolutePath,
                            isRecent = true,
                            lastOpenedTime = System.currentTimeMillis()
                        )
                        repository.insertDocument(doc)
                        Toast.makeText(context, "Saved as '$newName'", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error saving document", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    title = {
                        Column {
                            Text(text = fileName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(text = "${lines.size} lines • $wordCount words • Text Editor", fontSize = 11.sp, color = ColorTextGray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isFullScreen = true }) {
                            Icon(imageVector = Icons.Outlined.Fullscreen, contentDescription = "Full Screen")
                        }
                        IconButton(onClick = { showSaveAsDialog = true }) {
                            Icon(imageVector = Icons.Filled.Save, contentDescription = "Save As", tint = PrimaryBlue600)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                .background(Color(0xFFF8FAFC))
                .padding(if (isFullScreen) 0.dp else 16.dp)
                .testTag("text_editor_screen")
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Line numbers column
                    Column(
                        modifier = Modifier
                            .width(36.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(end = 8.dp)
                    ) {
                        lines.forEachIndexed { i, _ ->
                            Text(
                                text = "${i + 1}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }

                    // Main text editor field
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            if (isFullScreen) {
                IconButton(
                    onClick = { isFullScreen = false },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FullscreenExit,
                        contentDescription = "Exit Full Screen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
