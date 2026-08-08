package com.example.data.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfDocumentEngine : DocumentEngine {

    override fun canHandle(fileType: DocumentFileType): Boolean {
        return fileType == DocumentFileType.PDF
    }

    override suspend fun parseDocument(context: Context, uri: Uri): DocumentResult<ParsedDocumentContent> = withContext(Dispatchers.IO) {
        try {
            val pfd = getFileDescriptor(context, uri) ?: return@withContext DocumentResult.Error("Unable to open PDF file descriptor")
            val pdfRenderer = PdfRenderer(pfd)
            val pageCount = pdfRenderer.pageCount
            pdfRenderer.close()
            pfd.close()

            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "PDF Document"

            DocumentResult.Success(
                ParsedDocumentContent(
                    title = fileName,
                    fileType = DocumentFileType.PDF,
                    pageCount = pageCount,
                    textContent = "PDF Document with $pageCount page(s)",
                    isEditable = false, // True annotation supported separately
                    metadata = mapOf("Pages" to pageCount.toString(), "Type" to "Portable Document Format")
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Failed to parse PDF: ${e.message}", e)
        }
    }

    suspend fun renderPageBitmap(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd = getFileDescriptor(context, uri) ?: return@withContext null
            val renderer = PdfRenderer(pfd)
            if (pageIndex !in 0 until renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            // Fill background white before rendering
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createPdfFromImages(
        context: Context,
        imageUris: List<Uri>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()

            imageUris.forEachIndexed { index, uri ->
                val bitmap = loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                }
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createBlankPdf(outputFile: File, pageCount: Int = 1): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            for (i in 0 until pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, i + 1).create() // A4 standard size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(android.graphics.Color.WHITE)

                val paint = Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 14f
                    isAntiAlias = true
                }
                canvas.drawText("Page ${i + 1}", 50f, 800f, paint)

                pdfDocument.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun mergePdfs(
        context: Context,
        pdfUris: List<Uri>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val outputPdf = PdfDocument()
            var globalPageNumber = 1

            pdfUris.forEach { uri ->
                val pfd = getFileDescriptor(context, uri)
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    for (p in 0 until renderer.pageCount) {
                        val pdfPage = renderer.openPage(p)
                        val w = pdfPage.width
                        val h = pdfPage.height
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bmp)
                        c.drawColor(android.graphics.Color.WHITE)
                        pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pdfPage.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(w, h, globalPageNumber++).create()
                        val newPage = outputPdf.startPage(pageInfo)
                        newPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                        outputPdf.finishPage(newPage)
                        bmp.recycle()
                    }
                    renderer.close()
                    pfd.close()
                }
            }

            FileOutputStream(outputFile).use { out ->
                outputPdf.writeTo(out)
            }
            outputPdf.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun splitPdf(
        context: Context,
        pdfUri: Uri,
        splitPages: List<Int>, // 0-based page indices to extract into a new PDF
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = getFileDescriptor(context, pdfUri) ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val outputPdf = PdfDocument()

            splitPages.forEachIndexed { newIdx, originalIdx ->
                if (originalIdx in 0 until renderer.pageCount) {
                    val pdfPage = renderer.openPage(originalIdx)
                    val w = pdfPage.width
                    val h = pdfPage.height
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val c = Canvas(bmp)
                    c.drawColor(android.graphics.Color.WHITE)
                    pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, newIdx + 1).create()
                    val page = outputPdf.startPage(pageInfo)
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                    outputPdf.finishPage(page)
                    bmp.recycle()
                }
            }

            renderer.close()
            pfd.close()

            FileOutputStream(outputFile).use { out ->
                outputPdf.writeTo(out)
            }
            outputPdf.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun compressPdf(
        context: Context,
        pdfUri: Uri,
        outputFile: File,
        targetWidthScale: Float = 0.7f // scale down dimensions/resolution for size reduction
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = getFileDescriptor(context, pdfUri) ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val outputPdf = PdfDocument()

            for (p in 0 until renderer.pageCount) {
                val pdfPage = renderer.openPage(p)
                val targetW = (pdfPage.width * targetWidthScale).toInt().coerceAtLeast(300)
                val targetH = (pdfPage.height * targetWidthScale).toInt().coerceAtLeast(400)

                val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
                val c = Canvas(bmp)
                c.drawColor(android.graphics.Color.WHITE)
                pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pdfPage.close()

                val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, p + 1).create()
                val page = outputPdf.startPage(pageInfo)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                outputPdf.finishPage(page)
                bmp.recycle()
            }

            renderer.close()
            pfd.close()

            FileOutputStream(outputFile).use { out ->
                outputPdf.writeTo(out)
            }
            outputPdf.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun reorderPdfPages(
        context: Context,
        pdfUri: Uri,
        newPageOrder: List<Int>, // 0-based page index order
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = getFileDescriptor(context, pdfUri) ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val outputPdf = PdfDocument()

            newPageOrder.forEachIndexed { newIdx, originalIdx ->
                if (originalIdx in 0 until renderer.pageCount) {
                    val pdfPage = renderer.openPage(originalIdx)
                    val w = pdfPage.width
                    val h = pdfPage.height
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val c = Canvas(bmp)
                    c.drawColor(android.graphics.Color.WHITE)
                    pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, newIdx + 1).create()
                    val page = outputPdf.startPage(pageInfo)
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                    outputPdf.finishPage(page)
                    bmp.recycle()
                }
            }

            renderer.close()
            pfd.close()

            FileOutputStream(outputFile).use { out ->
                outputPdf.writeTo(out)
            }
            outputPdf.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "file") {
                ParcelFileDescriptor.open(File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
