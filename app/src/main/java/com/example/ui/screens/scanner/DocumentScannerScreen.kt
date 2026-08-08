package com.example.ui.screens.scanner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DocuProDatabase
import com.example.data.engine.OcrEngine
import com.example.data.engine.PdfDocumentEngine
import com.example.data.engine.ScanFilterMode
import com.example.data.engine.ScannerEngine
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.data.repository.DocumentRepository
import com.example.ui.theme.ColorPdfRed
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.PrimaryBlue600
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    onBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scannerEngine = remember { ScannerEngine() }
    val ocrEngine = remember { OcrEngine() }
    val pdfEngine = remember { PdfDocumentEngine() }
    val db = remember { DocuProDatabase.getDatabase(context) }
    val repository = remember { DocumentRepository(context, db.documentDao(), db.annotationDao(), db.scanDao()) }

    var scannedImageUri by remember { mutableStateOf<Uri?>(null) }
    var activeFilter by remember { mutableStateOf(ScanFilterMode.ENHANCE) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var extractedOcrText by remember { mutableStateOf("") }
    var isProcessingOcr by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scannedImageUri = uri
        }
    }

    fun saveScannedPdf() {
        val uri = scannedImageUri ?: return
        scope.launch {
            val pdfFile = File(context.filesDir, "Scan_${System.currentTimeMillis()}.pdf")
            pdfEngine.createPdfFromImages(context, listOf(uri), pdfFile)

            val doc = DocumentItem(
                uriString = Uri.fromFile(pdfFile).toString(),
                displayName = pdfFile.name,
                extension = "pdf",
                fileType = DocumentFileType.PDF,
                sizeBytes = pdfFile.length().coerceAtLeast(1024L),
                dateModified = System.currentTimeMillis(),
                filePath = pdfFile.absolutePath,
                contentSummary = extractedOcrText.take(150)
            )

            repository.insertDocument(doc)
            onOpenDocument(doc)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text(text = "Document Scanner & OCR", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("document_scanner_screen")
        ) {
            if (scannedImageUri == null) {
                // Initial Scan Launcher Area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(IndigoSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "Scan", tint = IndigoSecondary, modifier = Modifier.size(38.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Scan Physical Documents", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Capture pages with camera or select photos to enhance & extract text",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = "Gallery")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Photo", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Filter Preview Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[ Scanned Document Page - ${activeFilter.name} Mode ]",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Selector Buttons
                Text(text = "SCAN FILTERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanFilterMode.values().forEach { filter ->
                        val isSel = activeFilter == filter
                        OutlinedButton(
                            onClick = { activeFilter = filter },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSel) PrimaryBlue600 else Color.Transparent,
                                contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = filter.name.take(4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OCR Text Extraction Action
                Button(
                    onClick = {
                        val uri = scannedImageUri ?: return@Button
                        isProcessingOcr = true
                        scope.launch {
                            extractedOcrText = ocrEngine.extractTextFromImage(context, uri)
                            isProcessingOcr = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = "OCR", tint = PrimaryBlue600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isProcessingOcr) "Extracting Text..." else "Recognize Text (OCR)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                if (extractedOcrText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "OCR EXTRACTED TEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue600)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = extractedOcrText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save PDF Button
                Button(
                    onClick = { saveScannedPdf() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPdfRed)
                ) {
                    Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = "Save PDF", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Save as PDF Document", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
