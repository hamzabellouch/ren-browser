package com.tkno.ren.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tkno.ren.ui.theme.RenTheme
import com.tkno.ren.util.DownloadCategory
import com.tkno.ren.util.DownloadItem
import com.tkno.ren.util.DownloadStatus
import com.tkno.ren.util.DownloadsManager
import kotlinx.coroutines.delay

@Composable
fun DownloadsScreen(
    onClose: () -> Unit,
    onOpenItem: ((DownloadItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var downloadsList by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DownloadCategory.ALL) }
    var isEditMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<String>() }
    var showNewDownloadDialog by remember { mutableStateOf(false) }

    fun refreshList() {
        downloadsList = DownloadsManager.getDownloads(context)
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshList()
            delay(800)
        }
    }

    BackHandler {
        if (isEditMode) {
            isEditMode = false
            selectedItemIds.clear()
        } else {
            onClose()
        }
    }

    val filteredDownloads = remember(downloadsList, searchQuery, selectedCategory) {
        downloadsList.filter { item ->
            val matchesCategory = if (selectedCategory == DownloadCategory.ALL) {
                true
            } else {
                item.category == selectedCategory
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                item.fileName.contains(searchQuery.trim(), ignoreCase = true) ||
                        item.url.contains(searchQuery.trim(), ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    // Group items by date string
    val groupedDownloads = remember(filteredDownloads) {
        filteredDownloads.groupBy { item ->
            DownloadsManager.formatDateGroup(item.timestamp)
        }
    }

    RenTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (isEditMode) {
                            isEditMode = false
                            selectedItemIds.clear()
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Downloads",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Search Bar Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // Category Filter Chips
                val categories = DownloadCategory.values()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (cat in categories) {
                        val isSelected = selectedCategory == cat
                        val chipBg = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        }
                        val chipBorderColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                        val textColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(chipBg)
                                .border(BorderStroke(1.dp, chipBorderColor), RoundedCornerShape(percent = 50))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.label,
                                color = textColor,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Downloads List
                if (filteredDownloads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No downloads matching \"$searchQuery\"" else "No downloads",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        groupedDownloads.forEach { (dateHeader, itemsInGroup) ->
                            item(key = "header_$dateHeader") {
                                Text(
                                    text = dateHeader,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 8.dp)
                                )
                            }

                            items(itemsInGroup, key = { it.id }) { item ->
                                val isSelected = selectedItemIds.contains(item.id)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isEditMode) {
                                                if (isSelected) {
                                                    selectedItemIds.remove(item.id)
                                                } else {
                                                    selectedItemIds.add(item.id)
                                                }
                                            } else {
                                                if (onOpenItem != null) {
                                                    onOpenItem(item)
                                                } else {
                                                    DownloadsManager.openDownloadedFile(context, item)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 18.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // File Type Icon Box
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val iconVector = getCategoryIcon(item.category)
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = item.category.label,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // File Name, Status and Progress Bar
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = item.fileName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        val isDownloading = item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PENDING
                                        val isFailed = item.status == DownloadStatus.FAILED

                                        val subtitleText = when {
                                            isDownloading -> item.statusText.ifBlank { "Downloading..." }
                                            isFailed -> "Failed"
                                            else -> item.formattedSize
                                        }

                                        val subtitleColor = when {
                                            isFailed -> MaterialTheme.colorScheme.error
                                            isDownloading -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        }

                                        Text(
                                            text = subtitleText,
                                            color = subtitleColor,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (isDownloading) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            if (item.progress >= 0) {
                                                LinearProgressIndicator(
                                                    progress = { (item.progress / 100f).coerceIn(0f, 1f) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                                )
                                            }
                                        }
                                    }

                                    // Selection Circle in Edit Mode
                                    if (isEditMode) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .border(
                                                    BorderStroke(
                                                        2.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF484850)
                                                    ),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 84.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isEditMode) {
                            Text(
                                text = "New",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    showNewDownloadDialog = true
                                }
                            )

                            Text(
                                text = "Edit",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    isEditMode = true
                                    selectedItemIds.clear()
                                }
                            )
                        } else {
                            val allSelected = filteredDownloads.isNotEmpty() &&
                                    selectedItemIds.size == filteredDownloads.size

                            Text(
                                text = if (allSelected) "Deselect all" else "Select all",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    if (allSelected) {
                                        selectedItemIds.clear()
                                    } else {
                                        selectedItemIds.clear()
                                        selectedItemIds.addAll(filteredDownloads.map { it.id })
                                    }
                                }
                            )

                            val deleteCount = selectedItemIds.size
                            val deleteColor = if (deleteCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            Text(
                                text = if (deleteCount > 0) "Delete ($deleteCount)" else "Delete",
                                color = deleteColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable(enabled = deleteCount > 0) {
                                    DownloadsManager.deleteDownloads(context, selectedItemIds.toSet())
                                    selectedItemIds.clear()
                                    refreshList()
                                    Toast.makeText(context, "Deleted $deleteCount items", Toast.LENGTH_SHORT).show()
                                }
                            )

                            Text(
                                text = "Done",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    isEditMode = false
                                    selectedItemIds.clear()
                                }
                            )
                        }
                    }
                }
            }

            // "New" Download Dialog
            if (showNewDownloadDialog) {
                NewDownloadDialog(
                    onDismiss = { showNewDownloadDialog = false },
                    onDownload = { url, fileName ->
                        showNewDownloadDialog = false
                        val item = DownloadsManager.addDownload(context, url, fileName)
                        if (item != null) {
                            refreshList()
                            Toast.makeText(context, "Download started: ${item.fileName}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NewDownloadDialog(
    onDismiss: () -> Unit,
    onDownload: (url: String, fileName: String) -> Unit
) {
    var urlText by remember { mutableStateOf("https://") }
    var fileNameText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "New",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                // URL Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(0.dp, Color.Transparent)
                        )
                ) {
                    Column {
                        BasicTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // File Name Input Field
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        BasicTextField(
                            value = fileNameText,
                            onValueChange = { fileNameText = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (fileNameText.isEmpty()) {
                                    Text(
                                        text = "File name",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Download",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                if (urlText.isNotBlank() && urlText != "https://") {
                                    onDownload(urlText.trim(), fileNameText.trim())
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(category: DownloadCategory): ImageVector {
    return when (category) {
        DownloadCategory.VIDEOS -> Icons.Outlined.PlayArrow
        DownloadCategory.IMAGES -> Icons.Outlined.Image
        DownloadCategory.APK -> Icons.Outlined.Android
        DownloadCategory.DOCUMENTS -> Icons.Outlined.Description
        DownloadCategory.ARCHIVES -> Icons.Outlined.FolderZip
        DownloadCategory.ALL -> Icons.Outlined.Description
    }
}
