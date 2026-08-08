package com.example.ui.screens.docx

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.example.ads.NativeDocumentAdCard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.DocuProDatabase
import com.example.data.engine.WordDocumentEngine
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.data.repository.DocumentRepository
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
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var fileName by remember { mutableStateOf("Document.docx") }
    var showSaveAsDialog by remember { mutableStateOf(false) }

    // Text formatting options
    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isUnderlineActive by remember { mutableStateOf(false) }
    var fontSizeSp by remember { mutableIntStateOf(16) }
    var textAlign by remember { mutableStateOf(TextAlign.Left) }
    var selectedTextColorHex by remember { mutableStateOf("#1E293B") }
    var selectedHighlightHex by remember { mutableStateOf("#FFFFFF") }

    // Toolbar panels
    var activePanel by remember { mutableStateOf<FormattingPanel>(FormattingPanel.NONE) }

    // Inserted Pictures list
    var insertedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri: Uri? ->
        pickedUri?.let {
            insertedImages = insertedImages + it
            Toast.makeText(context, "Picture inserted into document!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(documentUri) {
        val result = wordEngine.parseDocument(context, uri)
        if (result is com.example.data.model.DocumentResult.Success) {
            textContent = result.data.textContent
            textFieldValue = TextFieldValue(text = textContent)
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
                            text = "$wordCount words • $charCount chars • WPS Executive Format",
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
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                // Secondary Panel (Color Picker / Style Picker / Font Size / Highlight)
                AnimatedVisibility(visible = activePanel != FormattingPanel.NONE) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (activePanel) {
                            FormattingPanel.COLOR -> {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Text Color Palette (Applies to Highlighted Text)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val colors = listOf(
                                            "#1E293B" to "Dark Slate",
                                            "#2563EB" to "Royal Blue",
                                            "#059669" to "Emerald",
                                            "#DC2626" to "Crimson",
                                            "#7C3AED" to "Purple",
                                            "#D97706" to "Amber",
                                            "#475569" to "Charcoal",
                                            "#000000" to "Pure Black"
                                        )
                                        items(colors) { (hex, name) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                                    .border(
                                                        width = if (selectedTextColorHex == hex) 2.dp else 0.dp,
                                                        color = ColorWordBlue,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        selectedTextColorHex = hex
                                                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                                                        textFieldValue = applyFormattingToSelection(textFieldValue, color = parsedColor)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            FormattingPanel.STYLE -> {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Paragraph Preset Style",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item {
                                            FilterChip(
                                                selected = fontSizeSp == 24 && isBoldActive,
                                                onClick = {
                                                    fontSizeSp = 24
                                                    isBoldActive = true
                                                    textFieldValue = applyFormattingToSelection(textFieldValue, fontSize = 24, isBold = true)
                                                },
                                                label = { Text("Heading 1 (24pt)") }
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = fontSizeSp == 20 && isBoldActive,
                                                onClick = {
                                                    fontSizeSp = 20
                                                    isBoldActive = true
                                                    textFieldValue = applyFormattingToSelection(textFieldValue, fontSize = 20, isBold = true)
                                                },
                                                label = { Text("Heading 2 (20pt)") }
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = fontSizeSp == 16 && isItalicActive,
                                                onClick = {
                                                    fontSizeSp = 16
                                                    isItalicActive = true
                                                    textFieldValue = applyFormattingToSelection(textFieldValue, fontSize = 16, isItalic = true)
                                                },
                                                label = { Text("Subtitle (16pt Italic)") }
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = fontSizeSp == 14 && !isBoldActive,
                                                onClick = {
                                                    fontSizeSp = 14
                                                    isBoldActive = false
                                                    isItalicActive = false
                                                    textFieldValue = applyFormattingToSelection(textFieldValue, fontSize = 14)
                                                },
                                                label = { Text("Body Text (14pt)") }
                                            )
                                        }
                                    }
                                }
                            }
                            FormattingPanel.HIGHLIGHT -> {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Background Highlight Color",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        val highlights = listOf(
                                            "#FFFFFF" to "None",
                                            "#FEF08A" to "Yellow",
                                            "#CFFAFE" to "Cyan",
                                            "#FFE4E6" to "Rose",
                                            "#DCFCE7" to "Green"
                                        )
                                        items(highlights) { (hex, label) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                                    .border(1.dp, Color.LightGray, CircleShape)
                                                    .clickable {
                                                        selectedHighlightHex = hex
                                                        val bgCol = if (hex == "#FFFFFF") Color.Transparent else Color(android.graphics.Color.parseColor(hex))
                                                        textFieldValue = applyFormattingToSelection(textFieldValue, backgroundColor = bgCol)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                // Primary Formatting Toolbar
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isBoldActive = !isBoldActive
                            textFieldValue = applyFormattingToSelection(textFieldValue, isBold = isBoldActive)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FormatBold,
                                contentDescription = "Bold",
                                tint = if (isBoldActive) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            isItalicActive = !isItalicActive
                            textFieldValue = applyFormattingToSelection(textFieldValue, isItalic = isItalicActive)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FormatItalic,
                                contentDescription = "Italic",
                                tint = if (isItalicActive) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            isUnderlineActive = !isUnderlineActive
                            textFieldValue = applyFormattingToSelection(textFieldValue, isUnderline = isUnderlineActive)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FormatUnderlined,
                                contentDescription = "Underline",
                                tint = if (isUnderlineActive) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Font Size adjusters
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val newSize = (fontSizeSp - 2).coerceAtLeast(10)
                                fontSizeSp = newSize
                                textFieldValue = applyFormattingToSelection(textFieldValue, fontSize = newSize)
                            }) {
                                Icon(imageVector = Icons.Filled.Remove, contentDescription = "Smaller Font", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${fontSizeSp}pt",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorWordBlue
                            )
                            IconButton(onClick = {
                                val newSize = (fontSizeSp + 2).coerceAtMost(36)
                                fontSizeSp = newSize
                                textFieldValue = applyFormattingToSelection(textFieldValue, fontSize = newSize)
                            }) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = "Larger Font", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Text Color Toggle
                        IconButton(onClick = {
                            activePanel = if (activePanel == FormattingPanel.COLOR) FormattingPanel.NONE else FormattingPanel.COLOR
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FormatColorText,
                                contentDescription = "Text Color",
                                tint = Color(android.graphics.Color.parseColor(selectedTextColorHex))
                            )
                        }

                        // Background Highlight Toggle
                        IconButton(onClick = {
                            activePanel = if (activePanel == FormattingPanel.HIGHLIGHT) FormattingPanel.NONE else FormattingPanel.HIGHLIGHT
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.FormatColorFill,
                                contentDescription = "Highlight",
                                tint = if (activePanel == FormattingPanel.HIGHLIGHT) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Insert Picture
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = "Insert Picture",
                                tint = ColorWordBlue
                            )
                        }

                        // Style Presets
                        IconButton(onClick = {
                            activePanel = if (activePanel == FormattingPanel.STYLE) FormattingPanel.NONE else FormattingPanel.STYLE
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Title,
                                contentDescription = "Style",
                                tint = if (activePanel == FormattingPanel.STYLE) ColorWordBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Alignment
                        IconButton(onClick = {
                            textAlign = when (textAlign) {
                                TextAlign.Left -> TextAlign.Center
                                TextAlign.Center -> TextAlign.Right
                                else -> TextAlign.Left
                            }
                        }) {
                            Icon(
                                imageVector = when (textAlign) {
                                    TextAlign.Center -> Icons.Filled.FormatAlignCenter
                                    TextAlign.Right -> Icons.Filled.FormatAlignRight
                                    else -> Icons.Filled.FormatAlignLeft
                                },
                                contentDescription = "Alignment",
                                tint = ColorWordBlue
                            )
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
            // Paper Layout Header (WPS style page margin header)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "A4 Executive Page • 2.5cm Margins", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Text(text = "Font ${fontSizeSp}pt", fontSize = 11.sp, color = ColorWordBlue, fontWeight = FontWeight.Bold)
                }
            }

            // Clean Executive Paper Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(720.dp),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Display inserted pictures inside document layout
                    if (insertedImages.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            items(insertedImages) { imageUri ->
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = "Inserted Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { insertedImages = insertedImages - imageUri },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove Picture",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            textContent = newValue.text
                        },
                        placeholder = { Text("Start typing your Word document content here...") },
                        modifier = Modifier.fillMaxSize(),
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

            // Native Ad after the last page/content of the Word document
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                NativeDocumentAdCard()
            }
        }
    }
}

private fun applyFormattingToSelection(
    currentValue: TextFieldValue,
    color: Color? = null,
    backgroundColor: Color? = null,
    isUnderline: Boolean = false,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    fontSize: Int? = null
): TextFieldValue {
    val annotated = currentValue.annotatedString
    val selection = currentValue.selection

    val start = if (!selection.collapsed) selection.min else 0
    val end = if (!selection.collapsed) selection.max else annotated.text.length

    if (start >= end || start < 0 || end > annotated.text.length) {
        return currentValue
    }

    val builder = AnnotatedString.Builder(annotated)

    if (color != null) {
        builder.addStyle(SpanStyle(color = color), start, end)
    }
    if (backgroundColor != null) {
        builder.addStyle(SpanStyle(background = backgroundColor), start, end)
    }
    if (isUnderline) {
        builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
    }
    if (isBold) {
        builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
    }
    if (isItalic) {
        builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
    }
    if (fontSize != null) {
        builder.addStyle(SpanStyle(fontSize = fontSize.sp), start, end)
    }

    return currentValue.copy(annotatedString = builder.toAnnotatedString())
}

private enum class FormattingPanel {
    NONE, COLOR, STYLE, HIGHLIGHT
}
