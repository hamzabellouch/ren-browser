package com.tkno.ren.ui

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tkno.ren.R
import com.tkno.ren.ui.theme.RenTheme
import com.tkno.ren.util.FaviconManager
import com.tkno.ren.util.HistoryItem
import com.tkno.ren.util.HistoryManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    onOpenUrl: (String) -> Unit,
    isIncognito: Boolean = com.tkno.ren.ui.theme.ThemeManager.isIncognitoActive,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var historyList by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var rotationTarget by remember { mutableFloatStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(durationMillis = 500),
        label = "HistoryRefreshRotation"
    )

    fun refresh(showToast: Boolean = false) {
        historyList = if (isIncognito) {
            HistoryManager.getIncognitoHistory()
        } else {
            HistoryManager.getHistory(context)
        }
        if (showToast) {
            Toast.makeText(context, context.getString(R.string.history_refreshed), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isIncognito) {
        refresh(showToast = false)
    }

    val displayList = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    RenTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = 20.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Incognito Icon + Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isIncognito) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_incognito_glasses),
                                contentDescription = "Incognito",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = if (isIncognito) "Incognito History" else "History",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Right Actions: Refresh, Search, Clear
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Refresh Button
                        IconButton(
                            onClick = {
                                if (!isRefreshing) {
                                    isRefreshing = true
                                    rotationTarget += 360f
                                    coroutineScope.launch {
                                        refresh(showToast = true)
                                        delay(500)
                                        isRefreshing = false
                                    }
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_reload),
                                contentDescription = stringResource(id = R.string.refresh),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotationAngle)
                            )
                        }

                        // Search Toggle Button
                        IconButton(
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search History",
                                tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // CLEAR Button
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable {
                                    if (isIncognito) {
                                        HistoryManager.clearIncognitoHistory()
                                        Toast.makeText(context, "Incognito history cleared", Toast.LENGTH_SHORT).show()
                                    } else {
                                        HistoryManager.clearHistory(context)
                                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                                    }
                                    refresh(showToast = false)
                                }
                        ) {
                            Text(
                                text = "CLEAR",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Search Bar (Animated Expand)
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.search_history),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                    )
                }

                if (isIncognito) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_incognito_glasses),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Incognito history is temporary and will be deleted automatically when you close incognito tabs or exit incognito mode.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                if (displayList.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = if (isIncognito) R.drawable.ic_incognito_glasses else R.drawable.ic_history),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(bottom = 12.dp)
                            )
                            Text(
                                text = when {
                                    searchQuery.isNotBlank() -> "No results matching \"$searchQuery\""
                                    isIncognito -> "No incognito browsing history"
                                    else -> "No browsing history"
                                },
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(displayList, key = { it.id }) { item ->
                            HistoryRowItem(
                                item = item,
                                onOpen = { onOpenUrl(item.url) },
                                onDelete = {
                                    if (isIncognito) {
                                        HistoryManager.deleteIncognitoHistoryItem(item.id)
                                    } else {
                                        HistoryManager.deleteHistoryItem(context, item.id)
                                    }
                                    refresh(showToast = false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRowItem(
    item: HistoryItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var faviconBitmap by remember(item.url) { mutableStateOf<Bitmap?>(FaviconManager.getFavicon(context, item.url)) }
    val knownIconRes = remember(item.url) { FaviconManager.getKnownIconRes(item.url) }

    LaunchedEffect(item.url) {
        if (knownIconRes == null && faviconBitmap == null) {
            FaviconManager.loadFaviconAsync(context, item.url) { bmp ->
                faviconBitmap = bmp
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onOpen() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Favicon Container
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            when {
                knownIconRes != null -> {
                    Image(
                        painter = painterResource(id = knownIconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                    )
                }
                faviconBitmap != null -> {
                    Image(
                        bitmap = faviconBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                    )
                }
                else -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_globe),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and URL text
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title.ifBlank { item.url },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.url,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Delete (X) Button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

class HistoryView(
    private val context: Context,
    private val onOpenUrl: (String) -> Unit
) {
    fun refreshList() {
    }
}
