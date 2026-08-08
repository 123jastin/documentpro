package com.example.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Files : Screen("files")
    object Recent : Screen("recent")
    object Starred : Screen("starred")
    object Settings : Screen("settings")

    object PdfViewer : Screen("pdf_viewer/{documentUri}") {
        fun createRoute(uri: String): String = "pdf_viewer/${Uri.encode(uri)}"
    }

    object DocxEditor : Screen("docx_editor/{documentUri}") {
        fun createRoute(uri: String): String = "docx_editor/${Uri.encode(uri)}"
    }

    object Spreadsheet : Screen("spreadsheet/{documentUri}") {
        fun createRoute(uri: String): String = "spreadsheet/${Uri.encode(uri)}"
    }

    object Presentation : Screen("presentation/{documentUri}") {
        fun createRoute(uri: String): String = "presentation/${Uri.encode(uri)}"
    }

    object TextEditor : Screen("text_editor/{documentUri}") {
        fun createRoute(uri: String): String = "text_editor/${Uri.encode(uri)}"
    }

    object DocumentScanner : Screen("scanner")
    object ImageToPdf : Screen("image_to_pdf")
    object PdfTools : Screen("pdf_tools")
    object NewDocument : Screen("new_document")
    object Search : Screen("search")

    object DocumentDetails : Screen("document_details/{documentUri}") {
        fun createRoute(uri: String): String = "document_details/${Uri.encode(uri)}"
    }
}
