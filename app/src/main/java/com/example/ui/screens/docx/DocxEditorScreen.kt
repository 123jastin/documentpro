package com.example.ui.screens.docx

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DocuProDatabase
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.data.repository.DocumentRepository
import com.example.data.engine.WordDocumentEngine
import com.example.ui.components.SaveAsDialog
import com.example.ui.theme.ColorWordBlue
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocxEditorScreen(
    documentUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wordEngine = remember { WordDocumentEngine() }
    val db = remember { DocuProDatabase.getDatabase(context) }
    val repository = remember { DocumentRepository(context, db.documentDao(), db.annotationDao(), db.scanDao()) }
    val uri = remember(documentUri) { Uri.parse(documentUri) }

    var textContent by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("Document.docx") }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isUnderlineActive by remember { mutableStateOf(false) }
    var fontSizeSp by remember { mutableIntStateOf(16) }
    var textAlign by remember { mutableStateOf(TextAlign.Left) }
    var selectedTextColorHex by remember { mutableStateOf("#1E293B") }

    LaunchedEffect(documentUri) {
        val result = wordEngine.parseDocument(context, uri)
        if (result is com.example.data.model.DocumentResult.Success) {
            textContent = result.data.textContent
            fileName = result.data.title
        }
    }

    val wordCount = remember(textContent) {
        if (textContent.isBlank()) 0 else textContent.trim().split("\\s+".toRegex()).size
    }
    val charCount = remember(textContent) { textContent.length }

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

                    val writeOk = wordEngine.writeWordDocument(context, targetUri, textContent)
                    if (writeOk) {
                        fileName = newName
                        val doc = DocumentItem(
                            uriString = targetUri.toString(),
                            displayName = newName,
                            extension = "docx",
                            fileType = DocumentFileType.WORD,
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
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = {
                    Column {
                        Text(text = fileName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            text = "$wordCount words • $charCount characters • WPS Word Format",
                            fontSize = 11.sp,
                            color = ColorWordBlue
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSaveAsDialog = true }) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = "Save As", tint = ColorWordBlue)
                    }
                    IconButton(onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, textContent)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Document"))
                    }) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isBoldActive = !isBoldActive }) {
                            Icon(
                                imageVector = Icons.Filled.FormatBold,
                                contentDescription = "Bold",
                                tint = if (isBoldActive) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isItalicActive = !isItalicActive }) {
                            Icon(
                                imageVector = Icons.Filled.FormatItalic,
                                contentDescription = "Italic",
                                tint = if (isItalicActive) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isUnderlineActive = !isUnderlineActive }) {
                            Icon(
                                imageVector = Icons.Filled.FormatUnderlined,
                                contentDescription = "Underline",
                                tint = if (isUnderlineActive) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { textAlign = TextAlign.Left }) {
                            Icon(
                                imageVector = Icons.Filled.FormatAlignLeft,
                                contentDescription = "Align Left",
                                tint = if (textAlign == TextAlign.Left) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { textAlign = TextAlign.Center }) {
                            Icon(
                                imageVector = Icons.Filled.FormatAlignCenter,
                                contentDescription = "Align Center",
                                tint = if (textAlign == TextAlign.Center) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { textAlign = TextAlign.Right }) {
                            Icon(
                                imageVector = Icons.Filled.FormatAlignRight,
                                contentDescription = "Align Right",
                                tint = if (textAlign == TextAlign.Right) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { textContent += "\n• " }) {
                            Icon(imageVector = Icons.Filled.FormatListBulleted, contentDescription = "Bullet List")
                        }
                        IconButton(onClick = {
                            val lineCount = textContent.lines().size
                            textContent += "\n$lineCount. "
                        }) {
                            Icon(imageVector = Icons.Filled.FormatListNumbered, contentDescription = "Numbered List")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("docx_editor_screen")
        ) {
            // Paper Layout Header (WPS style page margin top)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "A4 Page • Standard Margins", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Text(text = "100% Zoom", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Clean Professional Paper Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(680.dp),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    placeholder = { Text("Start typing your document here...") },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = TextStyle(
                        fontSize = fontSizeSp.sp,
                        fontWeight = if (isBoldActive) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalicActive) FontStyle.Italic else FontStyle.Normal,
                        textAlign = textAlign,
                        color = Color(android.graphics.Color.parseColor(selectedTextColorHex))
                    )
                )
            }
        }
    }
}
