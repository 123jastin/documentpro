package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileOptionBottomSheet(
    document: DocumentItem?,
    onDismiss: () -> Unit,
    onOpen: (DocumentItem) -> Unit,
    onDetails: (DocumentItem) -> Unit,
    onShare: (DocumentItem) -> Unit,
    onStar: (DocumentItem) -> Unit,
    onRename: (DocumentItem) -> Unit,
    onCopy: (DocumentItem) -> Unit,
    onMove: (DocumentItem) -> Unit,
    onDelete: (DocumentItem) -> Unit,
    onPrint: (DocumentItem) -> Unit
) {
    if (document == null) return

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = document.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Text(
                text = "${document.extension.uppercase()} • ${formatFileSize(document.sizeBytes)} • ${formatDate(document.dateModified)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            OptionRow(icon = Icons.Outlined.OpenInNew, title = "Open Document") {
                onDismiss()
                onOpen(document)
            }
            OptionRow(icon = Icons.Outlined.Info, title = "Document Details") {
                onDismiss()
                onDetails(document)
            }
            OptionRow(icon = Icons.Outlined.Share, title = "Share File") {
                onDismiss()
                onShare(document)
            }
            OptionRow(
                icon = Icons.Outlined.Star,
                title = if (document.isStarred) "Remove from Starred" else "Add to Starred"
            ) {
                onDismiss()
                onStar(document)
            }
            OptionRow(icon = Icons.Outlined.Edit, title = "Rename Document") {
                onDismiss()
                onRename(document)
            }
            OptionRow(icon = Icons.Outlined.FileCopy, title = "Make a Copy") {
                onDismiss()
                onCopy(document)
            }
            OptionRow(icon = Icons.Outlined.DriveFileMove, title = "Move File") {
                onDismiss()
                onMove(document)
            }
            OptionRow(icon = Icons.Outlined.Print, title = "Print") {
                onDismiss()
                onPrint(document)
            }
            OptionRow(
                icon = Icons.Outlined.Delete,
                title = "Move to Trash",
                tint = MaterialTheme.colorScheme.error
            ) {
                onDismiss()
                onDelete(document)
            }
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}
