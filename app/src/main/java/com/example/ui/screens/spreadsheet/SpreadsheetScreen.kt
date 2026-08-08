package com.example.ui.screens.spreadsheet

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.CellData
import com.example.data.engine.SheetData
import com.example.data.engine.SpreadsheetEngine
import com.example.ui.theme.ColorExcelGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetScreen(
    documentUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { SpreadsheetEngine() }
    val uri = remember(documentUri) { Uri.parse(documentUri) }

    var sheetData by remember { mutableStateOf<SheetData?>(null) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(Pair(0, 0)) }
    var formulaInput by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("Spreadsheet.xlsx") }
    var isFullScreen by remember { mutableStateOf(false) }

    LaunchedEffect(documentUri) {
        val result = engine.parseDocument(context, uri)
        if (result is com.example.data.model.DocumentResult.Success) {
            sheetData = result.data.spreadsheetData?.sheets?.firstOrNull() ?: engine.createSampleBudgetSheet()
            fileName = result.data.title
        } else {
            sheetData = engine.createSampleBudgetSheet()
        }
    }

    val currentSheet = sheetData ?: engine.createSampleBudgetSheet()
    val horizontalScrollState = rememberScrollState()

    fun getColName(colIdx: Int): String {
        return ('A' + colIdx).toString()
    }

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    title = {
                        Column {
                            Text(text = fileName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(text = "Spreadsheet Grid • ${currentSheet.rowCount}x${currentSheet.columnCount}", fontSize = 11.sp, color = ColorExcelGreen)
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
                        IconButton(onClick = { /* Save */ }) {
                            Icon(imageVector = Icons.Filled.Save, contentDescription = "Save", tint = ColorExcelGreen)
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isFullScreen) {
                // Formula Input Bar
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Functions, contentDescription = "Formula", tint = ColorExcelGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedCell != null) "${getColName(selectedCell!!.second)}${selectedCell!!.first + 1}" else "fx",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorExcelGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = formulaInput,
                            onValueChange = { formulaInput = it },
                            placeholder = { Text("Enter value or formula =SUM(...)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )
                        IconButton(onClick = {
                            val sel = selectedCell
                            if (sel != null) {
                                val mutableCells = currentSheet.cells.toMutableMap()
                                mutableCells[sel] = CellData(
                                    value = formulaInput,
                                    formula = if (formulaInput.startsWith("=")) formulaInput else null
                                )
                                sheetData = currentSheet.copy(cells = mutableCells)
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = "Apply", tint = ColorExcelGreen)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                .background(Color(0xFFF1F5F9))
                .testTag("spreadsheet_screen")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Grid Header Row (Column Letters)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                        .background(Color(0xFFCBD5E1))
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(36.dp)
                            .border(0.5.dp, Color.Gray),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(text = " ", fontSize = 11.sp)
                    }
                    for (col in 0 until currentSheet.columnCount) {
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(36.dp)
                                .border(0.5.dp, Color.Gray),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = getColName(col),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Grid Rows
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(currentSheet.rowCount) { rowIdx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            // Row Number Header
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(40.dp)
                                    .background(Color(0xFFE2E8F0))
                                    .border(0.5.dp, Color.Gray),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    text = "${rowIdx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                            }

                            // Cells in Row
                            for (colIdx in 0 until currentSheet.columnCount) {
                                val cellData = currentSheet.cells[Pair(rowIdx, colIdx)]
                                val isSelected = selectedCell == Pair(rowIdx, colIdx)

                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(40.dp)
                                        .background(if (isSelected) ColorExcelGreen.copy(alpha = 0.2f) else Color.White)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) ColorExcelGreen else Color.LightGray
                                        )
                                        .clickable {
                                            selectedCell = Pair(rowIdx, colIdx)
                                            formulaInput = cellData?.formula ?: cellData?.value ?: ""
                                        }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.CenterStart
                                ) {
                                    Text(
                                        text = cellData?.value ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = if (cellData?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.Black,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
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
