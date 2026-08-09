package com.example.ui.screens.imagetopdf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.ColorImagePurple
import com.example.ui.theme.ColorPdfRed
import com.example.ads.WatchAdDialog
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    onBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfEngine = remember { PdfDocumentEngine() }
    val db = remember { DocuProDatabase.getDatabase(context) }
    val repository = remember { DocumentRepository(context, db.documentDao(), db.annotationDao(), db.scanDao()) }

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showWatchAdDialog by remember { mutableStateOf(false) }

    val multiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
        }
    }

    fun convertImagesToPdf() {
        if (selectedImageUris.isEmpty()) return
        scope.launch {
            val pdfFile = File(context.filesDir, "Converted_Doc_${System.currentTimeMillis()}.pdf")
            pdfEngine.createPdfFromImages(context, selectedImageUris, pdfFile)

            val doc = DocumentItem(
                uriString = Uri.fromFile(pdfFile).toString(),
                displayName = pdfFile.name,
                extension = "pdf",
                fileType = DocumentFileType.PDF,
                sizeBytes = pdfFile.length().coerceAtLeast(1024L),
                dateModified = System.currentTimeMillis(),
                pageCount = selectedImageUris.size,
                filePath = pdfFile.absolutePath
            )

            repository.insertDocument(doc)
            onOpenDocument(doc)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text(text = "Image to PDF Converter", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                .padding(16.dp)
                .testTag("image_to_pdf_screen")
        ) {
            OutlinedButton(
                onClick = { multiPickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = ColorImagePurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Add Images (${selectedImageUris.size} Selected)", fontWeight = FontWeight.Bold, color = ColorImagePurple)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(selectedImageUris) { index, uri ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page ${index + 1}: ${uri.lastPathSegment?.takeLast(25) ?: "Image"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = {
                                val mutable = selectedImageUris.toMutableList()
                                mutable.removeAt(index)
                                selectedImageUris = mutable
                            }) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showWatchAdDialog) {
                WatchAdDialog(
                    title = "Convert Images to PDF",
                    featureName = "Image to PDF conversion",
                    onDismiss = { showWatchAdDialog = false },
                    onContinueWithAd = { convertImagesToPdf() }
                )
            }

            Button(
                onClick = { showWatchAdDialog = true },
                enabled = selectedImageUris.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPdfRed)
            ) {
                Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = "Convert", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Convert ${selectedImageUris.size} Images to PDF", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
