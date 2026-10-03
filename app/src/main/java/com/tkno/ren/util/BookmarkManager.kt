package com.tkno.ren.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class BookmarkFolder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val parentId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class BookmarkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val favicon: String? = null,
    val folderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class BookmarkImportResult(
    val importedBookmarks: Int,
    val importedFolders: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object BookmarkManager {

    private const val BOOKMARKS_FILE_NAME = "bookmarks.json"
    private const val PREFS_NAME = "ren_bookmarks_prefs"
    private const val KEY_INITIALIZED = "bookmarks_initialized"

    private val folderList = mutableListOf<BookmarkFolder>()
    private val bookmarkList = mutableListOf<BookmarkItem>()
    private var isLoaded = false
    private val lock = Any()

    /**
     * Initializes bookmarks and loads stored data into memory.
     */
    private fun ensureLoaded(context: Context) {
        synchronized(lock) {
            if (isLoaded) return

            val file = File(context.filesDir, BOOKMARKS_FILE_NAME)
            if (file.exists()) {
                try {
                    val content = file.readText(Charsets.UTF_8)
                    loadFromJsonString(content)
                    isLoaded = true
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // If empty or never initialized, populate default bookmarks
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val alreadyInit = prefs.getBoolean(KEY_INITIALIZED, false)

            if (!alreadyInit || (folderList.isEmpty() && bookmarkList.isEmpty())) {
                populateDefaults()
                prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
                saveToFile(context)
            }
            isLoaded = true
        }
    }

    private fun populateDefaults() {
        folderList.clear()
        bookmarkList.clear()

        val now = System.currentTimeMillis()
        val defaultItems = listOf(
            BookmarkItem(title = "Google", url = "https://www.google.com", timestamp = now),
            BookmarkItem(title = "Wikipedia", url = "https://www.wikipedia.org", timestamp = now + 1),
            BookmarkItem(title = "GitHub", url = "https://github.com", timestamp = now + 2),
            BookmarkItem(title = "DuckDuckGo", url = "https://duckduckgo.com", timestamp = now + 3),
            BookmarkItem(title = "Tor Project", url = "https://www.torproject.org", timestamp = now + 4),
            BookmarkItem(title = "Ren Browser", url = "https://github.com/hamzabellouch/ren-browser", timestamp = now + 5)
        )
        bookmarkList.addAll(defaultItems)
    }

    private fun loadFromJsonString(jsonString: String) {
        folderList.clear()
        bookmarkList.clear()

        val rootObj = JSONObject(jsonString)
        val foldersArray = rootObj.optJSONArray("folders") ?: JSONArray()
        for (i in 0 until foldersArray.length()) {
            val fObj = foldersArray.getJSONObject(i)
            val id = fObj.optString("id", UUID.randomUUID().toString())
            val title = fObj.optString("title", "Folder")
            val parentId = if (fObj.has("parentId") && !fObj.isNull("parentId")) {
                val p = fObj.optString("parentId")
                if (p.isNotBlank() && p != "null") p else null
            } else null
            val timestamp = fObj.optLong("timestamp", System.currentTimeMillis())

            folderList.add(BookmarkFolder(id, title, parentId, timestamp))
        }

        val bookmarksArray = rootObj.optJSONArray("bookmarks") ?: JSONArray()
        for (i in 0 until bookmarksArray.length()) {
            val bObj = bookmarksArray.getJSONObject(i)
            val id = bObj.optString("id", UUID.randomUUID().toString())
            val title = bObj.optString("title", "Untitled")
            val url = bObj.optString("url", "")
            val favicon = if (bObj.has("favicon") && !bObj.isNull("favicon")) bObj.optString("favicon") else null
            val folderId = if (bObj.has("folderId") && !bObj.isNull("folderId")) {
                val f = bObj.optString("folderId")
                if (f.isNotBlank() && f != "null") f else null
            } else null
            val timestamp = bObj.optLong("timestamp", System.currentTimeMillis())

            if (url.isNotBlank() && url != "about:blank") {
                bookmarkList.add(BookmarkItem(id, title, url, favicon, folderId, timestamp))
            }
        }
    }

    private fun saveToFile(context: Context) {
        synchronized(lock) {
            try {
                val rootObj = JSONObject()
                rootObj.put("version", 1)

                val foldersArray = JSONArray()
                for (f in folderList) {
                    val fObj = JSONObject().apply {
                        put("id", f.id)
                        put("title", f.title)
                        put("parentId", f.parentId ?: JSONObject.NULL)
                        put("timestamp", f.timestamp)
                    }
                    foldersArray.put(fObj)
                }
                rootObj.put("folders", foldersArray)

                val bookmarksArray = JSONArray()
                for (b in bookmarkList) {
                    val bObj = JSONObject().apply {
                        put("id", b.id)
                        put("title", b.title)
                        put("url", b.url)
                        put("favicon", b.favicon ?: JSONObject.NULL)
                        put("folderId", b.folderId ?: JSONObject.NULL)
                        put("timestamp", b.timestamp)
                    }
                    bookmarksArray.put(bObj)
                }
                rootObj.put("bookmarks", bookmarksArray)

                val file = File(context.filesDir, BOOKMARKS_FILE_NAME)
                file.writeText(rootObj.toString(2), Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // Folder CRUD Operations
    // ==========================================

    fun getFolders(context: Context): List<BookmarkFolder> {
        ensureLoaded(context)
        synchronized(lock) {
            return folderList.toList()
        }
    }

    fun getFolderById(context: Context, folderId: String): BookmarkFolder? {
        ensureLoaded(context)
        synchronized(lock) {
            return folderList.firstOrNull { it.id == folderId }
        }
    }

    fun getSubfolders(context: Context, parentId: String?): List<BookmarkFolder> {
        ensureLoaded(context)
        synchronized(lock) {
            return folderList
                .filter { it.parentId == parentId }
                .sortedBy { it.title.lowercase() }
        }
    }

    fun addFolder(context: Context, title: String, parentId: String? = null): BookmarkFolder {
        ensureLoaded(context)
        val cleanTitle = title.trim().ifBlank { "New Folder" }
        val newFolder = BookmarkFolder(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            parentId = parentId,
            timestamp = System.currentTimeMillis()
        )
        synchronized(lock) {
            folderList.add(newFolder)
            saveToFile(context)
        }
        return newFolder
    }

    fun updateFolder(context: Context, folderId: String, newTitle: String, newParentId: String? = null): Boolean {
        ensureLoaded(context)
        synchronized(lock) {
            val index = folderList.indexOfFirst { it.id == folderId }
            if (index != -1) {
                val current = folderList[index]
                folderList[index] = current.copy(
                    title = newTitle.trim().ifBlank { current.title },
                    parentId = newParentId ?: current.parentId
                )
                saveToFile(context)
                return true
            }
        }
        return false
    }

    fun deleteFolder(context: Context, folderId: String, recursive: Boolean = true): Boolean {
        ensureLoaded(context)
        synchronized(lock) {
            val folderToDelete = folderList.firstOrNull { it.id == folderId } ?: return false

            if (recursive) {
                // Collect all subfolder IDs recursively
                val allFolderIdsToDelete = mutableSetOf<String>()
                fun collectIds(id: String) {
                    allFolderIdsToDelete.add(id)
                    val children = folderList.filter { it.parentId == id }
                    for (c in children) {
                        collectIds(c.id)
                    }
                }
                collectIds(folderId)

                folderList.removeAll { allFolderIdsToDelete.contains(it.id) }
                bookmarkList.removeAll { it.folderId != null && allFolderIdsToDelete.contains(it.folderId) }
            } else {
                // Reparent subfolders and bookmarks to folderToDelete's parent
                val parent = folderToDelete.parentId
                for (i in folderList.indices) {
                    if (folderList[i].parentId == folderId) {
                        folderList[i] = folderList[i].copy(parentId = parent)
                    }
                }
                for (i in bookmarkList.indices) {
                    if (bookmarkList[i].folderId == folderId) {
                        bookmarkList[i] = bookmarkList[i].copy(folderId = parent)
                    }
                }
                folderList.removeAll { it.id == folderId }
            }

            saveToFile(context)
            return true
        }
    }

    fun getBreadcrumbs(context: Context, folderId: String?): List<BookmarkFolder> {
        ensureLoaded(context)
        if (folderId == null) return emptyList()

        val breadcrumbs = mutableListOf<BookmarkFolder>()
        synchronized(lock) {
            var currId: String? = folderId
            val visited = mutableSetOf<String>()
            while (currId != null && !visited.contains(currId)) {
                visited.add(currId)
                val folder = folderList.firstOrNull { it.id == currId } ?: break
                breadcrumbs.add(0, folder)
                currId = folder.parentId
            }
        }
        return breadcrumbs
    }

    fun getFolderPath(context: Context, folderId: String?): String {
        if (folderId == null) return "Root"
        val crumbs = getBreadcrumbs(context, folderId)
        return if (crumbs.isEmpty()) "Root" else crumbs.joinToString(" / ") { it.title }
    }

    // ==========================================
    // Bookmark CRUD Operations
    // ==========================================

    fun getBookmarks(context: Context): List<BookmarkItem> {
        ensureLoaded(context)
        synchronized(lock) {
            return bookmarkList.toList().sortedByDescending { it.timestamp }
        }
    }

    fun getBookmarkById(context: Context, id: String): BookmarkItem? {
        ensureLoaded(context)
        synchronized(lock) {
            return bookmarkList.firstOrNull { it.id == id }
        }
    }

    fun getBookmarkByUrl(context: Context, url: String): BookmarkItem? {
        ensureLoaded(context)
        if (url.isBlank() || url == "about:blank") return null
        val normalizedUrl = normalizeUrl(url)
        synchronized(lock) {
            return bookmarkList.firstOrNull { normalizeUrl(it.url) == normalizedUrl }
        }
    }

    fun isBookmarked(context: Context, url: String): Boolean {
        return getBookmarkByUrl(context, url) != null
    }

    fun getBookmarksInFolder(context: Context, folderId: String?): List<BookmarkItem> {
        ensureLoaded(context)
        synchronized(lock) {
            return bookmarkList
                .filter { it.folderId == folderId }
                .sortedByDescending { it.timestamp }
        }
    }

    fun addBookmark(
        context: Context,
        title: String,
        url: String,
        folderId: String? = null,
        favicon: String? = null
    ): BookmarkItem {
        ensureLoaded(context)
        val cleanUrl = url.trim()
        val cleanTitle = if (title.isBlank() || title == "Blank page") {
            try {
                val uri = android.net.Uri.parse(cleanUrl)
                uri.host ?: cleanUrl
            } catch (e: Exception) {
                cleanUrl
            }
        } else {
            title.trim()
        }

        val newItem = BookmarkItem(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            url = cleanUrl,
            favicon = favicon,
            folderId = folderId,
            timestamp = System.currentTimeMillis()
        )

        synchronized(lock) {
            // Remove existing duplicate with exact same URL if present
            bookmarkList.removeAll { normalizeUrl(it.url) == normalizeUrl(cleanUrl) }
            bookmarkList.add(0, newItem)
            saveToFile(context)
        }
        return newItem
    }

    fun updateBookmark(
        context: Context,
        id: String,
        title: String,
        url: String,
        folderId: String? = null,
        favicon: String? = null
    ): Boolean {
        ensureLoaded(context)
        synchronized(lock) {
            val index = bookmarkList.indexOfFirst { it.id == id }
            if (index != -1) {
                val current = bookmarkList[index]
                bookmarkList[index] = current.copy(
                    title = title.trim().ifBlank { current.title },
                    url = url.trim().ifBlank { current.url },
                    folderId = if (folderId != null) folderId else current.folderId,
                    favicon = favicon ?: current.favicon
                )
                saveToFile(context)
                return true
            }
        }
        return false
    }

    fun deleteBookmark(context: Context, id: String): Boolean {
        ensureLoaded(context)
        synchronized(lock) {
            val removed = bookmarkList.removeAll { it.id == id }
            if (removed) {
                saveToFile(context)
            }
            return removed
        }
    }

    fun deleteBookmarkByUrl(context: Context, url: String): Boolean {
        ensureLoaded(context)
        val normalized = normalizeUrl(url)
        synchronized(lock) {
            val removed = bookmarkList.removeAll { normalizeUrl(it.url) == normalized }
            if (removed) {
                saveToFile(context)
            }
            return removed
        }
    }

    fun toggleBookmark(context: Context, title: String, url: String, folderId: String? = null): Boolean {
        ensureLoaded(context)
        val existing = getBookmarkByUrl(context, url)
        return if (existing != null) {
            deleteBookmark(context, existing.id)
            false
        } else {
            addBookmark(context, title, url, folderId)
            true
        }
    }

    fun moveBookmark(context: Context, id: String, newFolderId: String?): Boolean {
        ensureLoaded(context)
        synchronized(lock) {
            val index = bookmarkList.indexOfFirst { it.id == id }
            if (index != -1) {
                bookmarkList[index] = bookmarkList[index].copy(folderId = newFolderId)
                saveToFile(context)
                return true
            }
        }
        return false
    }

    fun searchBookmarks(context: Context, query: String): List<BookmarkItem> {
        ensureLoaded(context)
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        synchronized(lock) {
            return bookmarkList.filter {
                it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
            }.sortedByDescending { it.timestamp }
        }
    }

    fun clearAll(context: Context) {
        synchronized(lock) {
            folderList.clear()
            bookmarkList.clear()
            saveToFile(context)
        }
    }

    // ==========================================
    // Netscape HTML & JSON Export
    // ==========================================

    fun exportToNetscapeHtml(context: Context): String {
        ensureLoaded(context)
        val sb = StringBuilder()
        sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
        sb.append("<!-- This is an automatically generated file.\n")
        sb.append("     It will be read and overwritten.\n")
        sb.append("     DO NOT EDIT! -->\n")
        sb.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
        sb.append("<TITLE>Bookmarks</TITLE>\n")
        sb.append("<H1>Bookmarks</H1>\n")
        sb.append("<DL><p>\n")

        synchronized(lock) {
            exportFolderChildrenHtml(context, null, sb, "    ")
        }

        sb.append("</DL><p>\n")
        return sb.toString()
    }

    private fun exportFolderChildrenHtml(
        context: Context,
        folderId: String?,
        sb: StringBuilder,
        indent: String
    ) {
        // 1. Export subfolders
        val subfolders = folderList.filter { it.parentId == folderId }.sortedBy { it.title }
        for (folder in subfolders) {
            val addDate = folder.timestamp / 1000
            val escapedTitle = escapeHtml(folder.title)
            sb.append("$indent<DT><H3 ADD_DATE=\"$addDate\" LAST_MODIFIED=\"$addDate\">$escapedTitle</H3>\n")
            sb.append("$indent<DL><p>\n")
            exportFolderChildrenHtml(context, folder.id, sb, "$indent    ")
            sb.append("$indent</DL><p>\n")
        }

        // 2. Export bookmarks in this folder
        val bookmarks = bookmarkList.filter { it.folderId == folderId }.sortedBy { it.title }
        for (item in bookmarks) {
            val addDate = item.timestamp / 1000
            val escapedUrl = escapeHtml(item.url)
            val escapedTitle = escapeHtml(item.title)
            val iconAttr = if (!item.favicon.isNullOrBlank()) " ICON=\"${escapeHtml(item.favicon)}\"" else ""
            sb.append("$indent<DT><A HREF=\"$escapedUrl\" ADD_DATE=\"$addDate\"$iconAttr>$escapedTitle</A>\n")
        }
    }

    fun exportToJson(context: Context): String {
        ensureLoaded(context)
        synchronized(lock) {
            val rootObj = JSONObject()
            rootObj.put("version", 1)

            val foldersArray = JSONArray()
            for (f in folderList) {
                foldersArray.put(JSONObject().apply {
                    put("id", f.id)
                    put("title", f.title)
                    put("parentId", f.parentId ?: JSONObject.NULL)
                    put("timestamp", f.timestamp)
                })
            }
            rootObj.put("folders", foldersArray)

            val bookmarksArray = JSONArray()
            for (b in bookmarkList) {
                bookmarksArray.put(JSONObject().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("url", b.url)
                    put("favicon", b.favicon ?: JSONObject.NULL)
                    put("folderId", b.folderId ?: JSONObject.NULL)
                    put("timestamp", b.timestamp)
                })
            }
            rootObj.put("bookmarks", bookmarksArray)
            return rootObj.toString(2)
        }
    }

    // ==========================================
    // Netscape HTML & JSON Import
    // ==========================================

    fun importFromJson(context: Context, jsonString: String, clearExisting: Boolean = false): BookmarkImportResult {
        ensureLoaded(context)
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.isBlank()) {
                return BookmarkImportResult(0, 0, false, "JSON content is empty")
            }

            var importedBookmarks = 0
            var importedFolders = 0

            synchronized(lock) {
                if (clearExisting) {
                    folderList.clear()
                    bookmarkList.clear()
                }

                if (trimmed.startsWith("[")) {
                    // Array of simple bookmarks: [{"title": "...", "url": "..."}]
                    val arr = JSONArray(trimmed)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val title = obj.optString("title", "Untitled")
                        val url = obj.optString("url", "")
                        if (url.isNotBlank() && url != "about:blank") {
                            addBookmarkInternal(title, url, null, null)
                            importedBookmarks++
                        }
                    }
                } else {
                    val rootObj = JSONObject(trimmed)
                    val foldersArray = rootObj.optJSONArray("folders") ?: JSONArray()
                    val folderIdMap = mutableMapOf<String, String>() // oldId -> newId

                    for (i in 0 until foldersArray.length()) {
                        val fObj = foldersArray.getJSONObject(i)
                        val oldId = fObj.optString("id", "")
                        val title = fObj.optString("title", "Folder")
                        val oldParentId = if (fObj.has("parentId") && !fObj.isNull("parentId")) fObj.optString("parentId") else null
                        val newParentId = if (oldParentId != null) folderIdMap[oldParentId] else null

                        val newFolder = BookmarkFolder(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            parentId = newParentId,
                            timestamp = fObj.optLong("timestamp", System.currentTimeMillis())
                        )
                        if (oldId.isNotBlank()) {
                            folderIdMap[oldId] = newFolder.id
                        }
                        folderList.add(newFolder)
                        importedFolders++
                    }

                    val bookmarksArray = rootObj.optJSONArray("bookmarks") ?: JSONArray()
                    for (i in 0 until bookmarksArray.length()) {
                        val bObj = bookmarksArray.getJSONObject(i)
                        val title = bObj.optString("title", "Untitled")
                        val url = bObj.optString("url", "")
                        val favicon = if (bObj.has("favicon") && !bObj.isNull("favicon")) bObj.optString("favicon") else null
                        val oldFolderId = if (bObj.has("folderId") && !bObj.isNull("folderId")) bObj.optString("folderId") else null
                        val newFolderId = if (oldFolderId != null) folderIdMap[oldFolderId] else null
                        val timestamp = bObj.optLong("timestamp", System.currentTimeMillis())

                        if (url.isNotBlank() && url != "about:blank") {
                            addBookmarkInternal(title, url, newFolderId, favicon, timestamp)
                            importedBookmarks++
                        }
                    }
                }

                saveToFile(context)
            }

            BookmarkImportResult(importedBookmarks, importedFolders, true)
        } catch (e: Exception) {
            BookmarkImportResult(0, 0, false, "JSON parse error: ${e.localizedMessage}")
        }
    }

    fun importFromNetscapeHtml(
        context: Context,
        htmlContent: String,
        targetFolderId: String? = null,
        clearExisting: Boolean = false
    ): BookmarkImportResult {
        ensureLoaded(context)
        return try {
            val content = htmlContent.trim()
            if (content.isBlank()) {
                return BookmarkImportResult(0, 0, false, "HTML content is empty")
            }

            var importedBookmarks = 0
            var importedFolders = 0

            synchronized(lock) {
                if (clearExisting) {
                    folderList.clear()
                    bookmarkList.clear()
                }

                // Stack tracking parent folder IDs: null is root or targetFolderId
                val folderStack = ArrayDeque<String?>()
                folderStack.addLast(targetFolderId)

                // Match both full elements and opening tags in stream order
                // Regex matches:
                // 1. <H3[^>]*>(.*?)</H3> or <h3 ...
                // 2. <A\s+([^>]*?)>(.*?)</A> or <a ...
                // 3. </DL> or </dl>
                val tagPattern = java.util.regex.Pattern.compile(
                    "(?i)(<h3\\b([^>]*)>(.*?)(?:</h3>|$))|(<a\\b([^>]*?)>(.*?)(?:</a>|$))|(</dl>)",
                    java.util.regex.Pattern.DOTALL
                )

                val matcher = tagPattern.matcher(content)
                while (matcher.find()) {
                    val fullMatch = matcher.group(0) ?: ""

                    if (matcher.group(1) != null) {
                        // Folder <H3 ...>Title</H3>
                        val attrs = matcher.group(2) ?: ""
                        val rawTitle = matcher.group(3) ?: "Folder"
                        val folderTitle = unescapeHtml(rawTitle.trim().replace(Regex("<[^>]*>"), ""))
                        val addDateSec = extractAttributeValue(attrs, "ADD_DATE")?.toLongOrNull()
                        val timestamp = if (addDateSec != null) {
                            if (addDateSec < 100000000000L) addDateSec * 1000 else addDateSec
                        } else System.currentTimeMillis()

                        val parent = if (folderStack.isNotEmpty()) folderStack.last() else targetFolderId
                        val newFolder = BookmarkFolder(
                            id = UUID.randomUUID().toString(),
                            title = folderTitle.ifBlank { "Folder" },
                            parentId = parent,
                            timestamp = timestamp
                        )
                        folderList.add(newFolder)
                        importedFolders++

                        // Push new folder onto stack for upcoming items until </DL>
                        folderStack.addLast(newFolder.id)
                    } else if (matcher.group(4) != null) {
                        // Bookmark <A ...>Title</A>
                        val attrs = matcher.group(5) ?: ""
                        val rawTitle = matcher.group(6) ?: ""
                        val bookmarkTitle = unescapeHtml(rawTitle.trim().replace(Regex("<[^>]*>"), ""))

                        val url = extractAttributeValue(attrs, "HREF")
                        val icon = extractAttributeValue(attrs, "ICON")
                        val addDateSec = extractAttributeValue(attrs, "ADD_DATE")?.toLongOrNull()
                        val timestamp = if (addDateSec != null) {
                            if (addDateSec < 100000000000L) addDateSec * 1000 else addDateSec
                        } else System.currentTimeMillis()

                        if (!url.isNullOrBlank() && url != "about:blank") {
                            val cleanUrl = unescapeHtml(url)
                            val currentFolder = if (folderStack.isNotEmpty()) folderStack.last() else targetFolderId
                            addBookmarkInternal(
                                title = bookmarkTitle.ifBlank { cleanUrl },
                                url = cleanUrl,
                                folderId = currentFolder,
                                favicon = icon,
                                timestamp = timestamp
                            )
                            importedBookmarks++
                        }
                    } else if (matcher.group(7) != null) {
                        // </DL> closing tag -> Pop folder stack (if deeper than base level)
                        if (folderStack.size > 1) {
                            folderStack.removeLast()
                        }
                    }
                }

                saveToFile(context)
            }

            BookmarkImportResult(importedBookmarks, importedFolders, true)
        } catch (e: Exception) {
            BookmarkImportResult(0, 0, false, "Import failed: ${e.localizedMessage}")
        }
    }

    private fun addBookmarkInternal(
        title: String,
        url: String,
        folderId: String?,
        favicon: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val cleanUrl = url.trim()
        val cleanTitle = title.trim().ifBlank { cleanUrl }
        // If already exists, remove older duplicate
        bookmarkList.removeAll { normalizeUrl(it.url) == normalizeUrl(cleanUrl) }
        bookmarkList.add(
            BookmarkItem(
                id = UUID.randomUUID().toString(),
                title = cleanTitle,
                url = cleanUrl,
                favicon = favicon,
                folderId = folderId,
                timestamp = timestamp
            )
        )
    }

    private fun extractAttributeValue(attrString: String, attrName: String): String? {
        val pattern = java.util.regex.Pattern.compile(
            "(?i)\\b$attrName\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))"
        )
        val m = pattern.matcher(attrString)
        if (m.find()) {
            return m.group(1) ?: m.group(2) ?: m.group(3)
        }
        return null
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&#x2F;", "/")
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim().lowercase()
        if (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        return clean
    }
}
