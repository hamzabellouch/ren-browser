package com.tkno.ren.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tkno.ren.R
import com.tkno.ren.ui.theme.RenTheme
import com.tkno.ren.ui.theme.ThemeManager
import com.tkno.ren.util.BookmarkFolder
import com.tkno.ren.util.BookmarkItem
import com.tkno.ren.util.BookmarkManager
import com.tkno.ren.util.FaviconManager
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onOpenBookmark: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit = {},
    onClose: () -> Unit = {},
    initialFolderId: String? = null,
    isIncognito: Boolean = ThemeManager.isIncognitoActive,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // Navigation State
    var currentFolderId by remember { mutableStateOf(initialFolderId) }
    var currentFolders by remember { mutableStateOf<List<BookmarkFolder>>(emptyList()) }
    var currentBookmarks by remember { mutableStateOf<List<BookmarkItem>>(emptyList()) }
    var breadcrumbs by remember { mutableStateOf<List<BookmarkFolder>>(emptyList()) }

    // Search State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<BookmarkItem>>(emptyList()) }

    // Dialog & Sheet States
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var bookmarkToEdit by remember { mutableStateOf<BookmarkItem?>(null) }
    var folderToEdit by remember { mutableStateOf<BookmarkFolder?>(null) }
    var bookmarkToDelete by remember { mutableStateOf<BookmarkItem?>(null) }
    var folderToDelete by remember { mutableStateOf<BookmarkFolder?>(null) }
    var bookmarkToMove by remember { mutableStateOf<BookmarkItem?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Context Action Bottom Sheets
    var selectedBookmarkForActions by remember { mutableStateOf<BookmarkItem?>(null) }
    var selectedFolderForActions by remember { mutableStateOf<BookmarkFolder?>(null) }

    // Menu States
    var showTopMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    // Refresh data function
    fun refresh() {
        if (isSearchActive && searchQuery.isNotBlank()) {
            searchResults = BookmarkManager.searchBookmarks(context, searchQuery)
        } else {
            currentFolders = BookmarkManager.getSubfolders(context, currentFolderId)
            currentBookmarks = BookmarkManager.getBookmarksInFolder(context, currentFolderId)
            breadcrumbs = BookmarkManager.getBreadcrumbs(context, currentFolderId)
        }
    }

    LaunchedEffect(currentFolderId, isSearchActive, searchQuery) {
        refresh()
    }

    // System Back Navigation Handling
    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else if (currentFolderId != null) {
            val crumbs = BookmarkManager.getBreadcrumbs(context, currentFolderId)
            if (crumbs.size > 1) {
                currentFolderId = crumbs[crumbs.size - 2].id
            } else {
                currentFolderId = null
            }
        } else {
            onClose()
        }
    }

    // File Import Launcher
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    val result = if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
                        BookmarkManager.importFromJson(context, content)
                    } else {
                        BookmarkManager.importFromNetscapeHtml(context, content, currentFolderId)
                    }

                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            "Imported ${result.importedBookmarks} bookmarks and ${result.importedFolders} folders",
                            Toast.LENGTH_SHORT
                        ).show()
                        refresh()
                    } else {
                        Toast.makeText(context, "Import failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // File Export Launcher (HTML)
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val html = BookmarkManager.exportToNetscapeHtml(context)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(html.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Bookmarks exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

                // ==========================================
                // Top Header / App Bar
                // ==========================================
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back / Close Icon
                            IconButton(
                                onClick = {
                                    if (isSearchActive) {
                                        isSearchActive = false
                                        searchQuery = ""
                                    } else if (currentFolderId != null) {
                                        val crumbs = BookmarkManager.getBreadcrumbs(context, currentFolderId)
                                        if (crumbs.size > 1) {
                                            currentFolderId = crumbs[crumbs.size - 2].id
                                        } else {
                                            currentFolderId = null
                                        }
                                    } else {
                                        onClose()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentFolderId != null || isSearchActive) {
                                        Icons.AutoMirrored.Outlined.ArrowBack
                                    } else {
                                        Icons.Default.Close
                                    },
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Title or Search Bar
                            if (isSearchActive) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(21.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = {
                                                searchQuery = it
                                                refresh()
                                            },
                                            textStyle = TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 15.sp
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            keyboardOptions = KeyboardOptions(
                                                imeAction = ImeAction.Search,
                                                keyboardType = KeyboardType.Text
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onSearch = { focusManager.clearFocus() }
                                            ),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "Search bookmarks...",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        fontSize = 15.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    searchQuery = ""
                                                    refresh()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear search",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentFolder = BookmarkManager.getFolderById(context, currentFolderId ?: "")
                                    Text(
                                        text = currentFolder?.title ?: "Bookmarks",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Search Toggle Action
                            if (!isSearchActive) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Add Action with Dropdown
                            Box {
                                IconButton(onClick = { showAddMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = showAddMenu,
                                    onDismissRequest = { showAddMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Add Bookmark") },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Bookmark, contentDescription = null)
                                        },
                                        onClick = {
                                            showAddMenu = false
                                            showAddBookmarkDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("New Folder") },
                                        leadingIcon = {
                                            Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                        },
                                        onClick = {
                                            showAddMenu = false
                                            showAddFolderDialog = true
                                        }
                                    )
                                }
                            }

                            // Overflow Menu
                            Box {
                                IconButton(onClick = { showTopMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Import bookmarks") },
                                        leadingIcon = {
                                            Icon(Icons.Default.FileUpload, contentDescription = null)
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            showImportDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export bookmarks") },
                                        leadingIcon = {
                                            Icon(Icons.Default.FileDownload, contentDescription = null)
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            showExportDialog = true
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Clear all bookmarks",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            showClearAllDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        // ==========================================
                        // Breadcrumbs Bar (when not searching)
                        // ==========================================
                        AnimatedVisibility(
                            visible = !isSearchActive && (currentFolderId != null || breadcrumbs.isNotEmpty()),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Root Chip
                                BreadcrumbChip(
                                    title = "Bookmarks",
                                    icon = Icons.Outlined.Home,
                                    isSelected = currentFolderId == null,
                                    onClick = { currentFolderId = null }
                                )

                                for (crumb in breadcrumbs) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    BreadcrumbChip(
                                        title = crumb.title,
                                        icon = Icons.Outlined.Folder,
                                        isSelected = crumb.id == currentFolderId,
                                        onClick = { currentFolderId = crumb.id }
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // Content Area: Folders & Bookmarks List
                // ==========================================
                if (isSearchActive) {
                    // Search Mode
                    if (searchResults.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            message = if (searchQuery.isBlank()) "Type to search bookmarks" else "No bookmarks found for \"$searchQuery\"",
                            actionText = null
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(searchResults, key = { it.id }) { bookmark ->
                                BookmarkRowItem(
                                    bookmark = bookmark,
                                    folderPath = BookmarkManager.getFolderPath(context, bookmark.folderId),
                                    onOpen = {
                                        onOpenBookmark(bookmark.url)
                                        onClose()
                                    },
                                    onMoreClick = { selectedBookmarkForActions = bookmark }
                                )
                            }
                        }
                    }
                } else {
                    // Folder Browsing Mode
                    val isEmpty = currentFolders.isEmpty() && currentBookmarks.isEmpty()
                    if (isEmpty) {
                        EmptyStateView(
                            icon = Icons.Outlined.BookmarkBorder,
                            message = if (currentFolderId == null) "No bookmarks yet" else "This folder is empty",
                            actionText = "Add Bookmark",
                            onAction = { showAddBookmarkDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Subfolders Section
                            if (currentFolders.isNotEmpty()) {
                                items(currentFolders, key = { it.id }) { folder ->
                                    val childCount = remember(folder.id) {
                                        BookmarkManager.getBookmarksInFolder(context, folder.id).size +
                                                BookmarkManager.getSubfolders(context, folder.id).size
                                    }
                                    FolderRowItem(
                                        folder = folder,
                                        itemCount = childCount,
                                        onClick = { currentFolderId = folder.id },
                                        onMoreClick = { selectedFolderForActions = folder }
                                    )
                                }
                            }

                            // 2. Bookmarks Section
                            if (currentBookmarks.isNotEmpty()) {
                                items(currentBookmarks, key = { it.id }) { bookmark ->
                                    BookmarkRowItem(
                                        bookmark = bookmark,
                                        folderPath = null,
                                        onOpen = {
                                            onOpenBookmark(bookmark.url)
                                            onClose()
                                        },
                                        onMoreClick = { selectedBookmarkForActions = bookmark }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Floating Action Button
            // ==========================================
            FloatingActionButton(
                onClick = { showAddBookmarkDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Bookmark"
                )
            }
        }
    }

    // ==========================================
    // Context Action Bottom Sheets
    // ==========================================

    // Bookmark Actions Bottom Sheet
    if (selectedBookmarkForActions != null) {
        val item = selectedBookmarkForActions!!
        ModalBottomSheet(
            onDismissRequest = { selectedBookmarkForActions = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                // Header: Favicon + Title + URL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FaviconBox(url = item.url, size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.url,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ActionMenuItem(
                    icon = Icons.Outlined.OpenInNew,
                    title = "Open in current tab",
                    onClick = {
                        selectedBookmarkForActions = null
                        onOpenBookmark(item.url)
                        onClose()
                    }
                )
                ActionMenuItem(
                    icon = Icons.Default.Add,
                    title = "Open in new tab",
                    onClick = {
                        selectedBookmarkForActions = null
                        onOpenInNewTab(item.url)
                        Toast.makeText(context, "Opened in new tab", Toast.LENGTH_SHORT).show()
                    }
                )
                ActionMenuItem(
                    icon = Icons.Outlined.ContentCopy,
                    title = "Copy link URL",
                    onClick = {
                        selectedBookmarkForActions = null
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("URL", item.url))
                        Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                ActionMenuItem(
                    icon = Icons.Outlined.Share,
                    title = "Share bookmark",
                    onClick = {
                        selectedBookmarkForActions = null
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, item.title)
                            putExtra(Intent.EXTRA_TEXT, item.url)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Bookmark"))
                    }
                )
                ActionMenuItem(
                    icon = Icons.Outlined.Edit,
                    title = "Edit bookmark",
                    onClick = {
                        val editTarget = item
                        selectedBookmarkForActions = null
                        bookmarkToEdit = editTarget
                    }
                )
                ActionMenuItem(
                    icon = Icons.Outlined.DriveFileMove,
                    title = "Move to folder...",
                    onClick = {
                        val moveTarget = item
                        selectedBookmarkForActions = null
                        bookmarkToMove = moveTarget
                    }
                )
                ActionMenuItem(
                    icon = Icons.Outlined.Delete,
                    title = "Delete bookmark",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        val deleteTarget = item
                        selectedBookmarkForActions = null
                        bookmarkToDelete = deleteTarget
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Folder Actions Bottom Sheet
    if (selectedFolderForActions != null) {
        val folder = selectedFolderForActions!!
        ModalBottomSheet(
            onDismissRequest = { selectedFolderForActions = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = folder.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ActionMenuItem(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Open folder",
                    onClick = {
                        selectedFolderForActions = null
                        currentFolderId = folder.id
                    }
                )
                ActionMenuItem(
                    icon = Icons.Outlined.Edit,
                    title = "Rename folder",
                    onClick = {
                        val renameTarget = folder
                        selectedFolderForActions = null
                        folderToEdit = renameTarget
                    }
                )
                ActionMenuItem(
                    icon = Icons.Outlined.Delete,
                    title = "Delete folder",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        val deleteTarget = folder
                        selectedFolderForActions = null
                        folderToDelete = deleteTarget
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // ==========================================
    // Dialogs
    // ==========================================

    // 1. Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        BookmarkEditDialog(
            title = "Add Bookmark",
            initialTitle = "",
            initialUrl = "https://",
            initialFolderId = currentFolderId,
            onDismiss = { showAddBookmarkDialog = false },
            onSave = { title, url, targetFolderId ->
                BookmarkManager.addBookmark(context, title, url, targetFolderId)
                showAddBookmarkDialog = false
                refresh()
                Toast.makeText(context, "Bookmark saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Edit Bookmark Dialog
    if (bookmarkToEdit != null) {
        val item = bookmarkToEdit!!
        BookmarkEditDialog(
            title = "Edit Bookmark",
            initialTitle = item.title,
            initialUrl = item.url,
            initialFolderId = item.folderId,
            onDismiss = { bookmarkToEdit = null },
            onSave = { title, url, targetFolderId ->
                BookmarkManager.updateBookmark(context, item.id, title, url, targetFolderId)
                bookmarkToEdit = null
                refresh()
                Toast.makeText(context, "Bookmark updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Add Folder Dialog
    if (showAddFolderDialog) {
        FolderEditDialog(
            title = "New Folder",
            initialTitle = "",
            onDismiss = { showAddFolderDialog = false },
            onSave = { title ->
                BookmarkManager.addFolder(context, title, currentFolderId)
                showAddFolderDialog = false
                refresh()
                Toast.makeText(context, "Folder created", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 4. Rename Folder Dialog
    if (folderToEdit != null) {
        val folder = folderToEdit!!
        FolderEditDialog(
            title = "Rename Folder",
            initialTitle = folder.title,
            onDismiss = { folderToEdit = null },
            onSave = { title ->
                BookmarkManager.updateFolder(context, folder.id, title)
                folderToEdit = null
                refresh()
                Toast.makeText(context, "Folder renamed", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 5. Delete Bookmark Confirm Dialog
    if (bookmarkToDelete != null) {
        val item = bookmarkToDelete!!
        AlertDialog(
            onDismissRequest = { bookmarkToDelete = null },
            title = { Text("Delete Bookmark") },
            text = { Text("Delete \"${item.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        BookmarkManager.deleteBookmark(context, item.id)
                        bookmarkToDelete = null
                        refresh()
                        Toast.makeText(context, "Bookmark deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Delete Folder Confirm Dialog
    if (folderToDelete != null) {
        val folder = folderToDelete!!
        val count = remember(folder.id) {
            BookmarkManager.getBookmarksInFolder(context, folder.id).size +
                    BookmarkManager.getSubfolders(context, folder.id).size
        }
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = {
                Text(
                    if (count > 0) {
                        "Delete \"${folder.title}\" and all of its $count items?"
                    } else {
                        "Delete folder \"${folder.title}\"?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        BookmarkManager.deleteFolder(context, folder.id, recursive = true)
                        folderToDelete = null
                        refresh()
                        Toast.makeText(context, "Folder deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 7. Move Bookmark Dialog
    if (bookmarkToMove != null) {
        val item = bookmarkToMove!!
        val allFolders = BookmarkManager.getFolders(context)
        AlertDialog(
            onDismissRequest = { bookmarkToMove = null },
            title = { Text("Move Bookmark") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    // Root option
                    item {
                        FolderSelectRow(
                            title = "Bookmarks (Root)",
                            isSelected = item.folderId == null,
                            onClick = {
                                BookmarkManager.moveBookmark(context, item.id, null)
                                bookmarkToMove = null
                                refresh()
                                Toast.makeText(context, "Moved to Root", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    items(allFolders) { folder ->
                        val path = BookmarkManager.getFolderPath(context, folder.id)
                        FolderSelectRow(
                            title = path,
                            isSelected = item.folderId == folder.id,
                            onClick = {
                                BookmarkManager.moveBookmark(context, item.id, folder.id)
                                bookmarkToMove = null
                                refresh()
                                Toast.makeText(context, "Moved to ${folder.title}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { bookmarkToMove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 8. Import Dialog
    if (showImportDialog) {
        ImportBookmarksDialog(
            onDismiss = { showImportDialog = false },
            onPickFile = {
                showImportDialog = false
                importFileLauncher.launch("*/*")
            },
            onPasteImport = { text ->
                showImportDialog = false
                val result = if (text.trim().startsWith("{") || text.trim().startsWith("[")) {
                    BookmarkManager.importFromJson(context, text)
                } else {
                    BookmarkManager.importFromNetscapeHtml(context, text, currentFolderId)
                }

                if (result.isSuccess) {
                    Toast.makeText(
                        context,
                        "Imported ${result.importedBookmarks} bookmarks and ${result.importedFolders} folders",
                        Toast.LENGTH_SHORT
                    ).show()
                    refresh()
                } else {
                    Toast.makeText(context, "Import failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // 9. Export Dialog
    if (showExportDialog) {
        ExportBookmarksDialog(
            onDismiss = { showExportDialog = false },
            onSaveFile = {
                showExportDialog = false
                exportFileLauncher.launch("ren_bookmarks.html")
            },
            onShareHtml = {
                showExportDialog = false
                val html = BookmarkManager.exportToNetscapeHtml(context)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/html"
                    putExtra(Intent.EXTRA_SUBJECT, "Ren Bookmarks Export")
                    putExtra(Intent.EXTRA_TEXT, html)
                }
                context.startActivity(Intent.createChooser(intent, "Share Bookmarks HTML"))
            },
            onCopyHtml = {
                showExportDialog = false
                val html = BookmarkManager.exportToNetscapeHtml(context)
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Bookmarks HTML", html))
                Toast.makeText(context, "Bookmarks HTML copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onCopyJson = {
                showExportDialog = false
                val json = BookmarkManager.exportToJson(context)
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Bookmarks JSON", json))
                Toast.makeText(context, "Bookmarks JSON copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 10. Clear All Confirm Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Bookmarks") },
            text = { Text("Are you sure you want to delete ALL bookmarks and folders? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        BookmarkManager.clearAll(context)
                        showClearAllDialog = false
                        refresh()
                        Toast.makeText(context, "All bookmarks cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// Sub-Components & Rows
// ==========================================

@Composable
private fun BreadcrumbChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRowItem(
    folder: BookmarkFolder,
    itemCount: Int,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onMoreClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$itemCount items",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRowItem(
    bookmark: BookmarkItem,
    folderPath: String?,
    onOpen: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onMoreClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaviconBox(url = bookmark.url, size = 40.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.title.ifBlank { bookmark.url },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = bookmark.url,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!folderPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = folderPath,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FaviconBox(url: String, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    var faviconBitmap by remember(url) { mutableStateOf<Bitmap?>(FaviconManager.getFavicon(context, url)) }
    val knownIconRes = remember(url) { FaviconManager.getKnownIconRes(url) }

    LaunchedEffect(url) {
        if (knownIconRes == null && faviconBitmap == null) {
            FaviconManager.loadFaviconAsync(context, url) { bmp ->
                faviconBitmap = bmp
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
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
                        .size(size * 0.55f)
                        .clip(CircleShape)
                )
            }
            faviconBitmap != null -> {
                Image(
                    bitmap = faviconBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(size * 0.55f)
                        .clip(CircleShape)
                )
            }
            else -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_globe),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 90.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onAction() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = actionText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FolderSelectRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ==========================================
// Dialog Implementations
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkEditDialog(
    title: String,
    initialTitle: String,
    initialUrl: String,
    initialFolderId: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit
) {
    val context = LocalContext.current
    var titleState by remember { mutableStateOf(initialTitle) }
    var urlState by remember { mutableStateOf(initialUrl) }
    var selectedFolderId by remember { mutableStateOf(initialFolderId) }
    var showFolderDropdown by remember { mutableStateOf(false) }
    val allFolders = remember { BookmarkManager.getFolders(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = titleState,
                    onValueChange = { titleState = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = urlState,
                    onValueChange = { urlState = it },
                    label = { Text("URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )

                // Folder Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = BookmarkManager.getFolderPath(context, selectedFolderId),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Folder") },
                        trailingIcon = {
                            Icon(Icons.Outlined.Folder, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFolderDropdown = true }
                    )
                    // Invisible overlay to capture clicks
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showFolderDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showFolderDropdown,
                        onDismissRequest = { showFolderDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Bookmarks (Root)") },
                            leadingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                            onClick = {
                                selectedFolderId = null
                                showFolderDropdown = false
                            }
                        )
                        for (f in allFolders) {
                            DropdownMenuItem(
                                text = { Text(BookmarkManager.getFolderPath(context, f.id)) },
                                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                onClick = {
                                    selectedFolderId = f.id
                                    showFolderDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (urlState.isNotBlank()) {
                        onSave(titleState.trim(), urlState.trim(), selectedFolderId)
                    }
                },
                enabled = urlState.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FolderEditDialog(
    title: String,
    initialTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var titleState by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = titleState,
                onValueChange = { titleState = it },
                label = { Text("Folder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (titleState.isNotBlank()) {
                        onSave(titleState.trim())
                    }
                },
                enabled = titleState.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ImportBookmarksDialog(
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onPasteImport: (String) -> Unit
) {
    var pasteText by remember { mutableStateOf("") }
    var isPasteMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Bookmarks") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isPasteMode) {
                    Text(
                        "You can import standard Netscape bookmark HTML files exported from Chrome, Firefox, Edge, or Safari, or JSON bookmark files.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPickFile() }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Choose Bookmark File",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Select HTML or JSON file from storage",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isPasteMode = true }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Paste HTML / JSON Content",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Paste bookmark data directly",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        label = { Text("Paste HTML or JSON text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 8
                    )
                }
            }
        },
        confirmButton = {
            if (isPasteMode) {
                TextButton(
                    onClick = { onPasteImport(pasteText) },
                    enabled = pasteText.isNotBlank()
                ) {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isPasteMode) isPasteMode = false else onDismiss()
                }
            ) {
                Text(if (isPasteMode) "Back" else "Cancel")
            }
        }
    )
}

@Composable
private fun ExportBookmarksDialog(
    onDismiss: () -> Unit,
    onSaveFile: () -> Unit,
    onShareHtml: () -> Unit,
    onCopyHtml: () -> Unit,
    onCopyJson: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Bookmarks") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Export bookmarks to Netscape HTML (compatible with all major browsers) or JSON format.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                ActionMenuItem(
                    icon = Icons.Default.FileDownload,
                    title = "Save HTML file to storage",
                    onClick = onSaveFile
                )
                ActionMenuItem(
                    icon = Icons.Outlined.Share,
                    title = "Share HTML content",
                    onClick = onShareHtml
                )
                ActionMenuItem(
                    icon = Icons.Outlined.ContentCopy,
                    title = "Copy HTML to clipboard",
                    onClick = onCopyHtml
                )
                ActionMenuItem(
                    icon = Icons.Outlined.ContentCopy,
                    title = "Copy JSON to clipboard",
                    onClick = onCopyJson
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
