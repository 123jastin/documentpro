package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.DocumentFileType
import com.example.data.model.DocumentItem
import com.example.ui.components.DocuProBottomNav
import com.example.ui.navigation.Screen
import com.example.ui.screens.details.DocumentDetailsScreen
import com.example.ui.screens.docx.DocxEditorScreen
import com.example.ui.screens.files.FileManagerScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.imagetopdf.ImageToPdfScreen
import com.example.ui.screens.newdoc.NewDocumentScreen
import com.example.ui.screens.pdf.PdfViewerScreen
import com.example.ui.screens.presentation.PresentationScreen
import com.example.ui.screens.recent.RecentScreen
import com.example.ui.screens.scanner.DocumentScannerScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.spreadsheet.SpreadsheetScreen
import com.example.ui.screens.starred.StarredScreen
import com.example.ui.screens.text.TextEditorScreen
import com.example.ui.theme.DocuProTheme

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.data.db.DocuProDatabase
import com.example.data.repository.DocumentRepository
import com.example.ui.screens.pdftools.PdfToolsScreen

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.ads.MobileAds
import com.example.ads.RewardedAdManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob SDK & preload Rewarded Ad
        MobileAds.initialize(this)
        RewardedAdManager.loadAd(this)

        val intentUri = intent?.data

        setContent {
            DocuProTheme {
                DocuProApp(initialUri = intentUri)
            }
        }
    }
}

@Composable
fun DocuProApp(initialUri: Uri? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = setOf(
        Screen.Home.route,
        Screen.Files.route,
        Screen.Recent.route,
        Screen.Starred.route,
        Screen.Settings.route
    )

    val scope = rememberCoroutineScope()
    val openPhoneFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val db = DocuProDatabase.getDatabase(context)
                val repo = DocumentRepository(context, db.documentDao(), db.annotationDao(), db.scanDao())
                val importedDoc = repo.importDocumentFromUri(uri)
                if (importedDoc != null) {
                    routeToDocumentViewer(importedDoc)
                }
            }
        }
    }

    fun routeToDocumentViewer(doc: DocumentItem) {
        val encodedUri = Uri.encode(doc.uriString)
        when (doc.fileType) {
            DocumentFileType.PDF -> navController.navigate("pdf_viewer/$encodedUri")
            DocumentFileType.WORD -> navController.navigate("docx_editor/$encodedUri")
            DocumentFileType.EXCEL -> navController.navigate("spreadsheet/$encodedUri")
            DocumentFileType.POWERPOINT -> navController.navigate("presentation/$encodedUri")
            DocumentFileType.TEXT -> navController.navigate("text_editor/$encodedUri")
            DocumentFileType.IMAGE -> navController.navigate("pdf_viewer/$encodedUri")
            else -> navController.navigate("text_editor/$encodedUri")
        }
    }

    // Auto open document when DocuPro is recommended / launched via Intent
    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            val db = DocuProDatabase.getDatabase(context)
            val repo = DocumentRepository(context, db.documentDao(), db.annotationDao(), db.scanDao())
            val importedDoc = repo.importDocumentFromUri(initialUri)
            if (importedDoc != null) {
                routeToDocumentViewer(importedDoc)
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                DocuProBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == Screen.Files.route) {
                            openPhoneFilesLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "text/plain",
                                    "image/*"
                                )
                            )
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                // Home Screen
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToNewDoc = { navController.navigate(Screen.NewDocument.route) },
                        onNavigateToScanner = { navController.navigate(Screen.DocumentScanner.route) },
                        onNavigateToImageToPdf = { navController.navigate(Screen.ImageToPdf.route) },
                        onNavigateToPdfTools = { toolIdx -> navController.navigate("pdf_tools/$toolIdx") },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToFilesCategory = { category ->
                            navController.navigate(Screen.Files.route)
                        },
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) },
                        onOpenDetails = { doc ->
                            navController.navigate("document_details/${Uri.encode(doc.uriString)}")
                        }
                    )
                }

                // File Manager Screen
                composable(Screen.Files.route) {
                    FileManagerScreen(
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) },
                        onOpenDetails = { doc ->
                            navController.navigate("document_details/${Uri.encode(doc.uriString)}")
                        }
                    )
                }

                // Recent Documents Screen
                composable(Screen.Recent.route) {
                    RecentScreen(
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) },
                        onOpenDetails = { doc ->
                            navController.navigate("document_details/${Uri.encode(doc.uriString)}")
                        }
                    )
                }

                // Starred Documents Screen
                composable(Screen.Starred.route) {
                    StarredScreen(
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) },
                        onOpenDetails = { doc ->
                            navController.navigate("document_details/${Uri.encode(doc.uriString)}")
                        }
                    )
                }

                // Settings Screen
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }

                // PDF Viewer & Annotator
                composable(
                    route = Screen.PdfViewer.route,
                    arguments = listOf(navArgument("documentUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val docUri = Uri.decode(backStackEntry.arguments?.getString("documentUri") ?: "")
                    PdfViewerScreen(
                        documentUri = docUri,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Word DOCX Editor
                composable(
                    route = Screen.DocxEditor.route,
                    arguments = listOf(navArgument("documentUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val docUri = Uri.decode(backStackEntry.arguments?.getString("documentUri") ?: "")
                    DocxEditorScreen(
                        documentUri = docUri,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Excel & CSV Spreadsheet
                composable(
                    route = Screen.Spreadsheet.route,
                    arguments = listOf(navArgument("documentUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val docUri = Uri.decode(backStackEntry.arguments?.getString("documentUri") ?: "")
                    SpreadsheetScreen(
                        documentUri = docUri,
                        onBack = { navController.popBackStack() }
                    )
                }

                // PowerPoint Presentation Viewer
                composable(
                    route = Screen.Presentation.route,
                    arguments = listOf(navArgument("documentUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val docUri = Uri.decode(backStackEntry.arguments?.getString("documentUri") ?: "")
                    PresentationScreen(
                        documentUri = docUri,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Plain Text Editor
                composable(
                    route = Screen.TextEditor.route,
                    arguments = listOf(navArgument("documentUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val docUri = Uri.decode(backStackEntry.arguments?.getString("documentUri") ?: "")
                    TextEditorScreen(
                        documentUri = docUri,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Document Camera Scanner
                composable(Screen.DocumentScanner.route) {
                    DocumentScannerScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) }
                    )
                }

                // Image to PDF Converter
                composable(Screen.ImageToPdf.route) {
                    ImageToPdfScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) }
                    )
                }

                // PDF Tools (Merge, Split, Compress, Reorder)
                composable(Screen.PdfTools.route) {
                    PdfToolsScreen(
                        initialToolIndex = 0,
                        onBack = { navController.popBackStack() },
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) }
                    )
                }

                composable(
                    route = "pdf_tools/{toolIndex}",
                    arguments = listOf(navArgument("toolIndex") { type = NavType.IntType })
                ) { backStackEntry ->
                    val toolIdx = backStackEntry.arguments?.getInt("toolIndex") ?: 0
                    PdfToolsScreen(
                        initialToolIndex = toolIdx,
                        onBack = { navController.popBackStack() },
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) }
                    )
                }

                // New Document & Templates
                composable(Screen.NewDocument.route) {
                    NewDocumentScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) }
                    )
                }

                // Global Document Search
                composable(Screen.Search.route) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) },
                        onOpenDetails = { doc ->
                            navController.navigate("document_details/${Uri.encode(doc.uriString)}")
                        }
                    )
                }

                // Document Details & Properties
                composable(
                    route = Screen.DocumentDetails.route,
                    arguments = listOf(navArgument("documentUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val docUri = Uri.decode(backStackEntry.arguments?.getString("documentUri") ?: "")
                    DocumentDetailsScreen(
                        documentUri = docUri,
                        onBack = { navController.popBackStack() },
                        onOpenDocument = { doc -> routeToDocumentViewer(doc) }
                    )
                }
            }
        }
    }
}
