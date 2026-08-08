package com.example.ui.screens.presentation

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.engine.PresentationEngine
import com.example.data.engine.PresentationModel
import com.example.ui.theme.ColorPptOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationScreen(
    documentUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { PresentationEngine() }
    val uri = remember(documentUri) { Uri.parse(documentUri) }

    var presentationModel by remember { mutableStateOf<PresentationModel?>(null) }
    var currentSlideIndex by remember { mutableIntStateOf(0) }
    var showNotes by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("Presentation.pptx") }

    LaunchedEffect(documentUri) {
        val result = engine.parseDocument(context, uri)
        if (result is com.example.data.model.DocumentResult.Success) {
            presentationModel = result.data.presentationData
            fileName = result.data.title
        } else {
            presentationModel = engine.createSamplePresentation(fileName)
        }
    }

    val model = presentationModel ?: engine.createSamplePresentation("Presentation.pptx")
    val slides = model.slides
    val currentSlide = slides.getOrNull(currentSlideIndex) ?: slides.first()

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    title = {
                        Column {
                            Text(text = fileName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                text = "Slide ${currentSlideIndex + 1} of ${slides.size}",
                                fontSize = 11.sp,
                                color = ColorPptOrange
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isFullScreen = true }) {
                            Icon(imageVector = Icons.Outlined.Fullscreen, contentDescription = "Full Screen Slide")
                        }
                        IconButton(onClick = { showNotes = !showNotes }) {
                            Icon(
                                imageVector = Icons.Filled.Notes,
                                contentDescription = "Presenter Notes",
                                tint = if (showNotes) ColorPptOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isFullScreen) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentSlideIndex > 0) currentSlideIndex-- },
                            enabled = currentSlideIndex > 0
                        ) {
                            Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Previous Slide")
                        }

                        LazyRow(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(slides) { idx, slide ->
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (idx == currentSlideIndex) ColorPptOrange else Color.White)
                                        .clickable { currentSlideIndex = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (idx == currentSlideIndex) Color.White else Color.Black
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { if (currentSlideIndex < slides.size - 1) currentSlideIndex++ },
                            enabled = currentSlideIndex < slides.size - 1
                        ) {
                            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Next Slide")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) PaddingValues(0.dp) else innerPadding)
                .background(Color(0xFF0F172A))
                .padding(if (isFullScreen) 0.dp else 16.dp)
                .testTag("presentation_screen")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Full Slide Card Canvas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isFullScreen) 32.dp else 24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentSlide.title,
                            fontSize = if (isFullScreen) 28.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentSlide.bodyText,
                            fontSize = if (isFullScreen) 18.sp else 15.sp,
                            lineHeight = if (isFullScreen) 28.sp else 24.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }

                if (showNotes && !isFullScreen) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(text = "PRESENTER NOTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorPptOrange)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = currentSlide.notes, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }

            if (isFullScreen) {
                IconButton(
                    onClick = { isFullScreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
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
