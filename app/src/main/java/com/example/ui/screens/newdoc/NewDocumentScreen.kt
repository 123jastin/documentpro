package com.example.ui.screens.newdoc

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import com.example.ui.components.TemplateCard
import com.example.ui.theme.ColorExcelGreen
import com.example.ui.theme.ColorPdfRed
import com.example.ui.theme.ColorPptOrange
import com.example.ui.theme.ColorTextGray
import com.example.ui.theme.ColorWordBlue
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDocumentScreen(
    onBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { DocuProDatabase.getDatabase(context) }
    val repository = remember {
        DocumentRepository(
            context,
            db.documentDao(),
            db.annotationDao(),
            db.scanDao()
        )
    }

    fun createAndOpen(name: String, ext: String, type: DocumentFileType, initialContent: String = "") {
        scope.launch {
            val docsDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
            val file = File(docsDir, "$name.$ext")

            if (type == DocumentFileType.PDF) {
                PdfDocumentEngine().createBlankPdf(file, pageCount = 1)
            } else {
                file.writeText(initialContent.ifBlank { "New $name document created with DocuPro." })
            }

            val doc = DocumentItem(
                uriString = Uri.fromFile(file).toString(),
                displayName = "$name.$ext",
                extension = ext,
                fileType = type,
                sizeBytes = file.length().coerceAtLeast(512L),
                dateModified = System.currentTimeMillis(),
                filePath = file.absolutePath
            )

            repository.insertDocument(doc)
            onOpenDocument(doc)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text(text = "New Document & Templates", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("new_document_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Blank Documents
            item {
                Text(
                    text = "BLANK DOCUMENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
            }
            item {
                TemplateCard(
                    title = "Blank PDF",
                    description = "Create a clean blank PDF canvas for annotations and notes",
                    icon = Icons.Outlined.PictureAsPdf,
                    brandColor = ColorPdfRed,
                    onClick = { createAndOpen("Untitled_Document_${System.currentTimeMillis() % 10000}", "pdf", DocumentFileType.PDF) }
                )
            }
            item {
                TemplateCard(
                    title = "Blank Word Document",
                    description = "Create a new .docx formatted Word document",
                    icon = Icons.Outlined.Description,
                    brandColor = ColorWordBlue,
                    onClick = { createAndOpen("Untitled_Word_${System.currentTimeMillis() % 10000}", "docx", DocumentFileType.WORD) }
                )
            }
            item {
                TemplateCard(
                    title = "Blank Spreadsheet",
                    description = "Create a new spreadsheet grid for budgets and calculations",
                    icon = Icons.Outlined.TableChart,
                    brandColor = ColorExcelGreen,
                    onClick = { createAndOpen("Untitled_Spreadsheet_${System.currentTimeMillis() % 10000}", "csv", DocumentFileType.EXCEL) }
                )
            }
            item {
                TemplateCard(
                    title = "Blank Presentation",
                    description = "Create slide decks for meetings and strategy",
                    icon = Icons.Outlined.Slideshow,
                    brandColor = ColorPptOrange,
                    onClick = { createAndOpen("Untitled_Presentation_${System.currentTimeMillis() % 10000}", "pptx", DocumentFileType.POWERPOINT) }
                )
            }
            item {
                TemplateCard(
                    title = "Blank Text Note",
                    description = "Create a lightweight plain text file",
                    icon = Icons.Outlined.Description,
                    brandColor = ColorTextGray,
                    onClick = { createAndOpen("Note_${System.currentTimeMillis() % 10000}", "txt", DocumentFileType.TEXT) }
                )
            }

            // Professional Templates
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "PROFESSIONAL TEMPLATES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
            }
            item {
                TemplateCard(
                    title = "Executive Resume / CV",
                    description = "Pre-formatted resume layout with experience sections",
                    icon = Icons.Outlined.Badge,
                    brandColor = ColorWordBlue,
                    onClick = { createAndOpen("Executive_Resume", "docx", DocumentFileType.WORD, "FULL NAME\nSenior Executive & Product Specialist\n\nWORK EXPERIENCE\n• Lead Product Engineer (2022 - Present)\n• Senior Systems Developer (2018 - 2022)\n\nEDUCATION\nB.S. Computer Science & Software Engineering") }
                )
            }
            item {
                TemplateCard(
                    title = "Cover Letter Template",
                    description = "Formal job application cover letter",
                    icon = Icons.Outlined.Article,
                    brandColor = ColorWordBlue,
                    onClick = { createAndOpen("Cover_Letter", "docx", DocumentFileType.WORD, "Date: August 8, 2026\nTo: Hiring Committee\n\nDear Hiring Team,\n\nI am writing to express my strong interest in the open leadership position...") }
                )
            }
            item {
                TemplateCard(
                    title = "Business Invoice",
                    description = "Itemized payment request and total calculation",
                    icon = Icons.Outlined.ReceiptLong,
                    brandColor = ColorExcelGreen,
                    onClick = { createAndOpen("Business_Invoice", "csv", DocumentFileType.EXCEL, "Item Description,Hours,Rate ($),Amount ($)\nSoftware Development,40,125.00,5000.00\nUI/UX Design,15,95.00,1425.00\nTotal Due,,,6425.00") }
                )
            }
        }
    }
}
