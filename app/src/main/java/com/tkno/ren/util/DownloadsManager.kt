package com.tkno.ren.util

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tkno.ren.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class DownloadCategory(val label: String) {
    ALL("All"),
    DOCUMENTS("Documents"),
    ARCHIVES("Archives"),
    APK("APK"),
    IMAGES("Images"),
    VIDEOS("Videos")
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val fileSize: Long = 0L,
    val formattedSize: String = "0 B",
    val mimeType: String = "",
    val category: DownloadCategory = DownloadCategory.ALL,
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String = "",
    val downloadId: Long = -1L,
    val status: DownloadStatus = DownloadStatus.COMPLETED,
    val progress: Int = 100, // 0 to 100, or -1 for indeterminate
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val statusText: String = ""
)

object DownloadsManager {

    private const val PREFS_NAME = "ren_browser_downloads_prefs"
    private const val KEY_DOWNLOADS_JSON = "browser_downloads_records"
    private const val KEY_DOWNLOAD_LOCATION_TYPE = "download_location_type"
    private const val KEY_DOWNLOAD_LOCATION_NAME = "download_location_name"
    private const val KEY_DOWNLOAD_LOCATION_URI = "download_location_uri"
    private const val KEY_DOWNLOAD_LOCATION_SUBPATH = "download_location_subpath"
    private const val KEY_DOWNLOAD_ENGINE = "download_engine"

    private const val DOWNLOAD_CHANNEL_ID = "ren_downloads_notification_channel"
    private val activeBuiltinDownloads = ConcurrentHashMap<String, DownloadItem>()

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isDownloadableUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val cleanUrl = url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        val downloadableExtensions = listOf(
            ".apk", ".xapk", ".apks",
            ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz", ".iso", ".dmg", ".exe", ".msi",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".epub",
            ".mp3", ".wav", ".flac", ".aac", ".ogg", ".opus", ".m4a",
            ".mp4", ".mkv", ".webm", ".avi", ".mov", ".wmv", ".3gp",
            ".torrent", ".bin"
        )
        return downloadableExtensions.any { cleanUrl.endsWith(it) }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager != null) {
                val existing = manager.getNotificationChannel(DOWNLOAD_CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        DOWNLOAD_CHANNEL_ID,
                        "Downloads",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Shows progress and completion of file downloads"
                        enableVibration(false)
                        setShowBadge(false)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }
    }

    fun getDownloadEngine(context: Context): String {
        return getPrefs(context).getString(KEY_DOWNLOAD_ENGINE, "system") ?: "system"
    }

    fun setDownloadEngine(context: Context, engine: String) {
        getPrefs(context).edit().putString(KEY_DOWNLOAD_ENGINE, engine).apply()
    }

    fun getDownloadEngineDisplayName(context: Context): String {
        return when (getDownloadEngine(context)) {
            "builtin" -> "Built-in download manager"
            else -> "System download manager"
        }
    }

    fun getDownloadLocationType(context: Context): String {
        return getPrefs(context).getString(KEY_DOWNLOAD_LOCATION_TYPE, "ren") ?: "ren"
    }

    fun getDownloadLocationDisplayName(context: Context): String {
        val type = getDownloadLocationType(context)
        val customName = getPrefs(context).getString(KEY_DOWNLOAD_LOCATION_NAME, null)
        if (!customName.isNullOrBlank()) return customName
        return when (type) {
            "default" -> "Downloads (Default)"
            "ren" -> "Ren / Downloads"
            else -> "Ren / Downloads"
        }
    }

    fun getDownloadLocationUri(context: Context): String? {
        return getPrefs(context).getString(KEY_DOWNLOAD_LOCATION_URI, null)
    }

    fun getCustomSubPath(context: Context): String {
        return getPrefs(context).getString(KEY_DOWNLOAD_LOCATION_SUBPATH, "") ?: ""
    }

    fun setDownloadLocation(
        context: Context,
        type: String,
        displayName: String,
        uriString: String? = null,
        subPath: String? = null
    ) {
        val editor = getPrefs(context).edit()
            .putString(KEY_DOWNLOAD_LOCATION_TYPE, type)
            .putString(KEY_DOWNLOAD_LOCATION_NAME, displayName)
        if (uriString != null) {
            editor.putString(KEY_DOWNLOAD_LOCATION_URI, uriString)
        } else {
            editor.remove(KEY_DOWNLOAD_LOCATION_URI)
        }
        if (subPath != null) {
            editor.putString(KEY_DOWNLOAD_LOCATION_SUBPATH, subPath)
        } else {
            editor.remove(KEY_DOWNLOAD_LOCATION_SUBPATH)
        }
        editor.apply()
    }

    fun getDownloads(context: Context): List<DownloadItem> {
        val jsonStr = getPrefs(context).getString(KEY_DOWNLOADS_JSON, null) ?: return activeBuiltinDownloads.values.toList()

        val list = mutableListOf<DownloadItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            var listModified = false

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val fileName = obj.optString("fileName", "Unknown")
                val url = obj.optString("url", "")
                var fileSize = obj.optLong("fileSize", 0L)
                val mimeType = obj.optString("mimeType", "")
                val categoryStr = obj.optString("category", DownloadCategory.ALL.name)
                val category = try {
                    DownloadCategory.valueOf(categoryStr)
                } catch (e: Exception) {
                    determineCategory(fileName, mimeType)
                }
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                var filePath = obj.optString("filePath", "")
                val downloadId = obj.optLong("downloadId", -1L)

                var status = DownloadStatus.COMPLETED
                var progress = 100
                var bytesDownloaded = fileSize
                var totalBytes = fileSize
                var statusText = formatFileSize(fileSize)

                // 1. Check if it's currently running in Built-in Downloader
                val activeBuiltin = activeBuiltinDownloads[id]
                if (activeBuiltin != null) {
                    status = activeBuiltin.status
                    progress = activeBuiltin.progress
                    bytesDownloaded = activeBuiltin.bytesDownloaded
                    totalBytes = activeBuiltin.totalBytes
                    statusText = activeBuiltin.statusText
                    filePath = activeBuiltin.filePath
                } else if (downloadId > 0 && dm != null) {
                    // 2. Check System DownloadManager status
                    try {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = dm.query(query)
                        if (cursor != null && cursor.moveToFirst()) {
                            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val bytesCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val localUriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                            val dmStatus = if (statusCol >= 0) cursor.getInt(statusCol) else -1
                            val bytesSoFar = if (bytesCol >= 0) cursor.getLong(bytesCol) else 0L
                            val total = if (totalCol >= 0) cursor.getLong(totalCol) else 0L
                            val localUriStr = if (localUriCol >= 0) cursor.getString(localUriCol) else null

                            bytesDownloaded = bytesSoFar
                            totalBytes = total

                            when (dmStatus) {
                                DownloadManager.STATUS_RUNNING -> {
                                    status = DownloadStatus.DOWNLOADING
                                    progress = if (total > 0) ((bytesSoFar * 100) / total).toInt().coerceIn(0, 100) else -1
                                    statusText = if (total > 0) {
                                        "Downloading $progress% (${formatFileSize(bytesSoFar)} / ${formatFileSize(total)})"
                                    } else {
                                        "Downloading... (${formatFileSize(bytesSoFar)})"
                                    }
                                }
                                DownloadManager.STATUS_PENDING -> {
                                    status = DownloadStatus.PENDING
                                    progress = 0
                                    statusText = "Waiting in queue..."
                                }
                                DownloadManager.STATUS_PAUSED -> {
                                    status = DownloadStatus.PENDING
                                    progress = if (total > 0) ((bytesSoFar * 100) / total).toInt().coerceIn(0, 100) else 0
                                    statusText = "Paused (${formatFileSize(bytesSoFar)})"
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    status = DownloadStatus.COMPLETED
                                    progress = 100
                                    val realSize = if (total > 0) total else bytesSoFar
                                    if (realSize > 0 && realSize != fileSize) {
                                        fileSize = realSize
                                        listModified = true
                                    }
                                    statusText = formatFileSize(fileSize)
                                    if (filePath.isBlank() && !localUriStr.isNullOrBlank()) {
                                        try {
                                            val uri = Uri.parse(localUriStr)
                                            val p = uri.path
                                            if (!p.isNullOrBlank()) {
                                                filePath = p
                                                listModified = true
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    status = DownloadStatus.FAILED
                                    progress = 0
                                    statusText = "Download failed"
                                }
                            }
                            cursor.close()
                        }
                    } catch (_: Exception) {}
                } else {
                    // Check if file exists on disk
                    if (filePath.isNotBlank()) {
                        val f = File(filePath)
                        if (f.exists() && f.length() > 0 && fileSize <= 0) {
                            fileSize = f.length()
                            statusText = formatFileSize(fileSize)
                            listModified = true
                        }
                    }
                }

                list.add(
                    DownloadItem(
                        id = id,
                        fileName = fileName,
                        url = url,
                        fileSize = fileSize,
                        formattedSize = if (fileSize > 0) formatFileSize(fileSize) else statusText,
                        mimeType = mimeType,
                        category = category,
                        timestamp = timestamp,
                        filePath = filePath,
                        downloadId = downloadId,
                        status = status,
                        progress = progress,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        statusText = statusText
                    )
                )
            }

            if (listModified) {
                saveDownloads(context, list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun addDownload(
        context: Context,
        url: String,
        customFileName: String? = null,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        referer: String? = null
    ): DownloadItem? {
        if (url.isBlank()) return null

        ensureNotificationChannel(context)
        val trimmedUrl = url.trim()

        // 1. Handle Base64 Data URLs (e.g. data:image/png;base64,...)
        if (trimmedUrl.startsWith("data:", ignoreCase = true)) {
            return handleDataUrlDownload(context, trimmedUrl, customFileName)
        }

        // 2. Prepare HTTP/HTTPS URL
        val finalUrl = if (!trimmedUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedUrl.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmedUrl"
        } else {
            trimmedUrl
        }

        // 3. Resolve filename and clean invalid characters
        var inferredName = customFileName?.trim()?.takeIf { it.isNotBlank() }
            ?: URLUtil.guessFileName(finalUrl, contentDisposition, mimeType).takeIf { it.isNotBlank() }
            ?: Uri.parse(finalUrl).lastPathSegment?.takeIf { it.isNotBlank() }
            ?: "download_${System.currentTimeMillis()}"

        inferredName = sanitizeFileName(inferredName)

        // 4. Resolve MIME Type & Category
        val resolvedMimeType = if (!mimeType.isNullOrBlank() && mimeType != "*/*") {
            mimeType
        } else {
            val ext = MimeTypeMap.getFileExtensionFromUrl(finalUrl).lowercase(Locale.ROOT)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
        val category = determineCategory(inferredName, resolvedMimeType)

        // 5. Determine Destination Directory & Path
        val locationType = getDownloadLocationType(context)
        val destinationSubPath = when (locationType) {
            "default" -> inferredName
            "ren" -> "Ren/$inferredName"
            "custom" -> {
                val customSub = getCustomSubPath(context)
                if (customSub.isNotBlank()) "$customSub/$inferredName" else inferredName
            }
            else -> "Ren/$inferredName"
        }

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = if (locationType == "ren") File(downloadsDir, "Ren") else downloadsDir
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val expectedFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            destinationSubPath
        )

        val selectedEngine = getDownloadEngine(context)

        // 6A. BUILT-IN DOWNLOAD MANAGER ENGINE
        if (selectedEngine == "builtin") {
            return startBuiltinDownload(
                context = context,
                url = finalUrl,
                fileName = inferredName,
                mimeType = resolvedMimeType,
                category = category,
                targetFile = expectedFile,
                userAgent = userAgent,
                referer = referer
            )
        }

        // 6B. SYSTEM DOWNLOAD MANAGER ENGINE
        var downloadId = -1L
        try {
            val request = DownloadManager.Request(Uri.parse(finalUrl)).apply {
                setTitle(inferredName)
                setDescription("Downloading $inferredName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, destinationSubPath)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                if (resolvedMimeType.isNotBlank() && resolvedMimeType != "*/*") {
                    setMimeType(resolvedMimeType)
                }

                // Inject Cookies from WebView for authenticated downloads
                val cookies = CookieManager.getInstance().getCookie(finalUrl)
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }

                // Inject User-Agent
                val activeUserAgent = if (!userAgent.isNullOrBlank()) {
                    userAgent
                } else {
                    try {
                        WebSettings.getDefaultUserAgent(context)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (!activeUserAgent.isNullOrBlank()) {
                    addRequestHeader("User-Agent", activeUserAgent)
                }

                // Inject Referer
                if (!referer.isNullOrBlank() && referer.startsWith("http", ignoreCase = true)) {
                    addRequestHeader("Referer", referer)
                }
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm != null) {
                downloadId = dm.enqueue(request)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }

        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            fileName = inferredName,
            url = finalUrl,
            fileSize = 0L,
            formattedSize = "Queued",
            mimeType = resolvedMimeType,
            category = category,
            timestamp = System.currentTimeMillis(),
            filePath = expectedFile.absolutePath,
            downloadId = downloadId,
            status = DownloadStatus.PENDING,
            progress = 0,
            statusText = "Queued in System"
        )

        val currentList = getDownloads(context).toMutableList()
        currentList.removeAll { it.id == item.id }
        currentList.add(0, item)
        saveDownloads(context, currentList)
        return item
    }

    private fun startBuiltinDownload(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String,
        category: DownloadCategory,
        targetFile: File,
        userAgent: String?,
        referer: String?
    ): DownloadItem {
        val itemId = UUID.randomUUID().toString()
        val notificationId = itemId.hashCode()

        var destinationFile = targetFile
        var counter = 1
        val parentDir = destinationFile.parentFile ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!parentDir.exists()) parentDir.mkdirs()

        val baseWithoutExt = fileName.substringBeforeLast(".")
        val fileExt = fileName.substringAfterLast(".", "")

        while (destinationFile.exists()) {
            val newName = if (fileExt.isNotBlank()) "$baseWithoutExt($counter).$fileExt" else "$baseWithoutExt($counter)"
            destinationFile = File(parentDir, newName)
            counter++
        }

        val initialItem = DownloadItem(
            id = itemId,
            fileName = destinationFile.name,
            url = url,
            fileSize = 0L,
            formattedSize = "Starting...",
            mimeType = mimeType,
            category = category,
            timestamp = System.currentTimeMillis(),
            filePath = destinationFile.absolutePath,
            downloadId = -1L,
            status = DownloadStatus.DOWNLOADING,
            progress = 0,
            statusText = "Starting download..."
        )

        activeBuiltinDownloads[itemId] = initialItem

        val currentList = getDownloads(context).toMutableList()
        currentList.removeAll { it.id == itemId }
        currentList.add(0, initialItem)
        saveDownloads(context, currentList)

        // Show starting notification
        updateBuiltinNotification(
            context = context,
            notificationId = notificationId,
            fileName = destinationFile.name,
            progress = 0,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            isDone = false,
            isFailed = false
        )

        // Launch download in background
        val finalTargetFile = destinationFile
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                var currentUrlStr = url
                var redirects = 0
                while (redirects < 5) {
                    val conn = URL(currentUrlStr).openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000

                    val cookies = CookieManager.getInstance().getCookie(currentUrlStr)
                    if (!cookies.isNullOrBlank()) {
                        conn.setRequestProperty("Cookie", cookies)
                    }
                    val activeUserAgent = userAgent ?: try {
                        WebSettings.getDefaultUserAgent(context)
                    } catch (e: Exception) {
                        "Mozilla/5.0 (Android; Mobile)"
                    }
                    conn.setRequestProperty("User-Agent", activeUserAgent)
                    if (!referer.isNullOrBlank()) {
                        conn.setRequestProperty("Referer", referer)
                    }
                    conn.setRequestProperty("Accept", "*/*")
                    conn.setRequestProperty("Accept-Encoding", "identity")

                    conn.connect()
                    val responseCode = conn.responseCode
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308
                    ) {
                        val newLocation = conn.getHeaderField("Location")
                        if (!newLocation.isNullOrBlank()) {
                            currentUrlStr = newLocation
                            redirects++
                            conn.disconnect()
                            continue
                        }
                    }
                    connection = conn
                    break
                }

                val conn = connection ?: throw Exception("Failed to open connection")
                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    throw Exception("HTTP error code: $responseCode")
                }

                val contentLength = conn.contentLengthLong
                inputStream = conn.inputStream
                outputStream = FileOutputStream(finalTargetFile)

                val buffer = ByteArray(16384)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastNotificationTime = System.currentTimeMillis()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastNotificationTime >= 400) {
                        lastNotificationTime = now
                        val currentProgress = if (contentLength > 0) {
                            ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }
                        val statusText = if (contentLength > 0) {
                            "Downloading $currentProgress% (${formatFileSize(totalBytesRead)} / ${formatFileSize(contentLength)})"
                        } else {
                            "Downloading... (${formatFileSize(totalBytesRead)})"
                        }

                        val progressItem = initialItem.copy(
                            status = DownloadStatus.DOWNLOADING,
                            progress = currentProgress,
                            bytesDownloaded = totalBytesRead,
                            totalBytes = contentLength,
                            statusText = statusText
                        )
                        activeBuiltinDownloads[itemId] = progressItem

                        updateBuiltinNotification(
                            context = context,
                            notificationId = notificationId,
                            fileName = finalTargetFile.name,
                            progress = currentProgress,
                            bytesDownloaded = totalBytesRead,
                            totalBytes = contentLength,
                            isDone = false,
                            isFailed = false
                        )
                    }
                }
                outputStream.flush()

                val finalSize = finalTargetFile.length()
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(finalTargetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )

                val completedItem = DownloadItem(
                    id = itemId,
                    fileName = finalTargetFile.name,
                    url = url,
                    fileSize = finalSize,
                    formattedSize = formatFileSize(finalSize),
                    mimeType = mimeType,
                    category = category,
                    timestamp = System.currentTimeMillis(),
                    filePath = finalTargetFile.absolutePath,
                    downloadId = -1L,
                    status = DownloadStatus.COMPLETED,
                    progress = 100,
                    bytesDownloaded = finalSize,
                    totalBytes = finalSize,
                    statusText = formatFileSize(finalSize)
                )

                activeBuiltinDownloads.remove(itemId)
                val updatedList = getDownloads(context).toMutableList()
                val idx = updatedList.indexOfFirst { it.id == itemId }
                if (idx >= 0) {
                    updatedList[idx] = completedItem
                } else {
                    updatedList.add(0, completedItem)
                }
                saveDownloads(context, updatedList)

                updateBuiltinNotification(
                    context = context,
                    notificationId = notificationId,
                    fileName = finalTargetFile.name,
                    progress = 100,
                    bytesDownloaded = finalSize,
                    totalBytes = finalSize,
                    isDone = true,
                    isFailed = false,
                    filePath = finalTargetFile.absolutePath
                )

                mainHandler.post {
                    Toast.makeText(context, "Download complete: ${finalTargetFile.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activeBuiltinDownloads.remove(itemId)
                val updatedList = getDownloads(context).toMutableList()
                val idx = updatedList.indexOfFirst { it.id == itemId }
                if (idx >= 0) {
                    updatedList[idx] = updatedList[idx].copy(
                        status = DownloadStatus.FAILED,
                        progress = 0,
                        formattedSize = "Failed",
                        statusText = "Failed: ${e.localizedMessage}"
                    )
                    saveDownloads(context, updatedList)
                }

                updateBuiltinNotification(
                    context = context,
                    notificationId = notificationId,
                    fileName = finalTargetFile.name,
                    progress = 0,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                    isDone = false,
                    isFailed = true
                )

                mainHandler.post {
                    Toast.makeText(context, "Built-in download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { outputStream?.close() } catch (_: Exception) {}
                try { inputStream?.close() } catch (_: Exception) {}
                try { connection?.disconnect() } catch (_: Exception) {}
            }
        }

        return initialItem
    }

    private fun updateBuiltinNotification(
        context: Context,
        notificationId: Int,
        fileName: String,
        progress: Int,
        bytesDownloaded: Long,
        totalBytes: Long,
        isDone: Boolean,
        isFailed: Boolean,
        filePath: String? = null
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            ensureNotificationChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_downloads)
                .setContentTitle(fileName)
                .setOnlyAlertOnce(true)
                .setAutoCancel(isDone || isFailed)

            if (isDone) {
                builder.setContentText("Download complete (${formatFileSize(bytesDownloaded)})")
                    .setProgress(0, 0, false)
                    .setOngoing(false)

                if (!filePath.isNullOrBlank()) {
                    val file = File(filePath)
                    if (file.exists()) {
                        val uri = try {
                            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        } catch (e: Exception) {
                            Uri.fromFile(file)
                        }
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "*/*")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            notificationId,
                            viewIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                        )
                        builder.setContentIntent(pendingIntent)
                    }
                }
            } else if (isFailed) {
                builder.setContentText("Download failed")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
            } else {
                val progressText = if (totalBytes > 0) {
                    "${formatFileSize(bytesDownloaded)} / ${formatFileSize(totalBytes)} ($progress%)"
                } else {
                    formatFileSize(bytesDownloaded)
                }
                builder.setContentText(progressText)
                    .setProgress(100, if (progress >= 0) progress else 0, totalBytes <= 0)
                    .setOngoing(true)
            }

            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDataUrlDownload(
        context: Context,
        dataUrl: String,
        customFileName: String?
    ): DownloadItem? {
        try {
            val commaIndex = dataUrl.indexOf(',')
            if (commaIndex == -1) return null

            val header = dataUrl.substring(0, commaIndex)
            val base64Data = dataUrl.substring(commaIndex + 1)

            val mimeType = if (header.contains(";")) {
                header.substringAfter("data:").substringBefore(";")
            } else {
                header.substringAfter("data:")
            }.ifBlank { "image/png" }

            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "png"
            var fileName = customFileName?.trim()?.takeIf { it.isNotBlank() }
                ?: "image_${System.currentTimeMillis()}.$ext"

            if (!fileName.contains(".")) {
                fileName = "$fileName.$ext"
            }
            fileName = sanitizeFileName(fileName)

            val bytes = Base64.decode(base64Data, Base64.DEFAULT)

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val renDir = File(downloadsDir, "Ren")
            if (!renDir.exists()) renDir.mkdirs()

            var targetFile = File(renDir, fileName)
            var counter = 1
            val baseWithoutExt = fileName.substringBeforeLast(".")
            val fileExt = fileName.substringAfterLast(".", "")
            while (targetFile.exists()) {
                val newName = if (fileExt.isNotBlank()) "$baseWithoutExt($counter).$fileExt" else "$baseWithoutExt($counter)"
                targetFile = File(renDir, newName)
                counter++
            }

            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType),
                null
            )

            val fileSize = targetFile.length()
            val category = determineCategory(targetFile.name, mimeType)

            val item = DownloadItem(
                id = UUID.randomUUID().toString(),
                fileName = targetFile.name,
                url = "data:$mimeType",
                fileSize = fileSize,
                formattedSize = formatFileSize(fileSize),
                mimeType = mimeType,
                category = category,
                timestamp = System.currentTimeMillis(),
                filePath = targetFile.absolutePath,
                downloadId = -1L,
                status = DownloadStatus.COMPLETED,
                progress = 100,
                bytesDownloaded = fileSize,
                totalBytes = fileSize,
                statusText = formatFileSize(fileSize)
            )

            val currentList = getDownloads(context).toMutableList()
            currentList.add(0, item)
            saveDownloads(context, currentList)

            Toast.makeText(context, "Saved: ${targetFile.name}", Toast.LENGTH_SHORT).show()
            return item
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    fun addSavedPageDownload(
        context: Context,
        title: String,
        originalUrl: String,
        filePath: String,
        fileSize: Long
    ): DownloadItem {
        val fileName = if (filePath.isNotBlank()) File(filePath).name else "$title.mht"
        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            url = originalUrl,
            fileSize = fileSize,
            formattedSize = formatFileSize(fileSize),
            mimeType = "multipart/related",
            category = DownloadCategory.DOCUMENTS,
            timestamp = System.currentTimeMillis(),
            filePath = filePath,
            status = DownloadStatus.COMPLETED,
            progress = 100,
            bytesDownloaded = fileSize,
            totalBytes = fileSize,
            statusText = formatFileSize(fileSize)
        )
        val currentList = getDownloads(context).toMutableList()
        currentList.add(0, item)
        saveDownloads(context, currentList)
        return item
    }

    fun deleteDownloads(context: Context, idsToDelete: Set<String>) {
        val currentList = getDownloads(context).toMutableList()
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        for (id in idsToDelete) {
            activeBuiltinDownloads.remove(id)
            val item = currentList.find { it.id == id }
            if (item != null) {
                if (item.downloadId > 0 && dm != null) {
                    try {
                        dm.remove(item.downloadId)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                if (item.filePath.isNotBlank()) {
                    try {
                        val file = File(item.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
        currentList.removeAll { idsToDelete.contains(it.id) }
        saveDownloads(context, currentList)
    }

    fun openDownloadedFile(context: Context, item: DownloadItem) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

            // 1. Try DownloadManager Content URI
            if (item.downloadId > 0 && dm != null) {
                val contentUri = dm.getUriForDownloadedFile(item.downloadId)
                if (contentUri != null) {
                    val mime = dm.getMimeTypeForDownloadedFile(item.downloadId) ?: item.mimeType.ifBlank { "*/*" }
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, mime)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    return
                }
            }

            // 2. Try Local File using FileProvider
            if (item.filePath.isNotBlank()) {
                val file = File(item.filePath)
                if (file.exists()) {
                    val uri = try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(file)
                    }
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, item.mimeType.ifBlank { "*/*" })
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    return
                }
            }

            // 3. Fallback: Open URL or Android Downloads system screen
            if (item.url.isNotBlank() && !item.url.startsWith("data:") && item.url != "about:blank") {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatDateGroup(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("EEE, MMM dd", Locale.ENGLISH)
        return sdf.format(date)
    }

    fun determineCategory(fileName: String, mimeType: String): DownloadCategory {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val lowerMime = mimeType.lowercase(Locale.ROOT)

        return when {
            lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".webm") ||
            lowerName.endsWith(".avi") || lowerName.endsWith(".mov") || lowerName.endsWith(".3gp") ||
            lowerMime.startsWith("video/") -> DownloadCategory.VIDEOS

            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") ||
            lowerName.endsWith(".gif") || lowerName.endsWith(".webp") || lowerName.endsWith(".svg") ||
            lowerName.endsWith(".bmp") || lowerMime.startsWith("image/") -> DownloadCategory.IMAGES

            lowerName.endsWith(".apk") || lowerName.endsWith(".xapk") || lowerName.endsWith(".apks") ||
            lowerMime.contains("android.package-archive") -> DownloadCategory.APK

            lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".7z") ||
            lowerName.endsWith(".tar") || lowerName.endsWith(".gz") || lowerName.endsWith(".bz2") ||
            lowerMime.contains("zip") || lowerMime.contains("compressed") || lowerMime.contains("tar") -> DownloadCategory.ARCHIVES

            lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx") ||
            lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".ppt") ||
            lowerName.endsWith(".pptx") || lowerName.endsWith(".txt") || lowerName.endsWith(".epub") ||
            lowerMime.startsWith("text/") || lowerMime.contains("pdf") || lowerMime.contains("document") -> DownloadCategory.DOCUMENTS

            else -> DownloadCategory.ALL
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
    }

    private fun saveDownloads(context: Context, items: List<DownloadItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("fileName", item.fileName)
                put("url", item.url)
                put("fileSize", item.fileSize)
                put("formattedSize", item.formattedSize)
                put("mimeType", item.mimeType)
                put("category", item.category.name)
                put("timestamp", item.timestamp)
                put("filePath", item.filePath)
                put("downloadId", item.downloadId)
            }
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_DOWNLOADS_JSON, jsonArray.toString()).apply()
    }
}
