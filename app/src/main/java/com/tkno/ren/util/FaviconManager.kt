package com.tkno.ren.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.tkno.ren.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors

object FaviconManager {

    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(100) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun extractDomain(url: String): String {
        if (url.isBlank()) return ""
        return try {
            val uri = URI(if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url)
            val host = uri.host ?: ""
            host.removePrefix("www.").removePrefix("m.").lowercase()
        } catch (e: Exception) {
            val clean = url.replace("https://", "").replace("http://", "").split("/").firstOrNull() ?: url
            clean.removePrefix("www.").removePrefix("m.").lowercase()
        }
    }

    fun getKnownIconRes(url: String): Int? {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.equals("about:blank", ignoreCase = true) || trimmed.equals("Blank page", ignoreCase = true)) {
            return R.drawable.ic_google_g
        }
        val domain = extractDomain(trimmed)
        if (domain.isBlank()) return R.drawable.ic_google_g

        return when {
            domain.contains("google.") || domain == "google" || trimmed.contains("google.com/search") || trimmed.contains("/search?q=") -> R.drawable.ic_google_g
            domain.contains("youtube.com") || domain == "youtu.be" -> R.drawable.ic_youtube
            domain.contains("github.com") -> R.drawable.ic_github
            domain.contains("facebook.com") || domain == "fb.com" || domain == "messenger.com" -> R.drawable.ic_facebook
            domain.contains("instagram.com") -> R.drawable.ic_instagram
            domain.contains("reddit.com") -> R.drawable.ic_reddit
            domain.contains("linkedin.com") -> R.drawable.ic_linkedin
            domain.contains("bsky.app") || domain.contains("bluesky") -> R.drawable.ic_bluesky
            domain.contains("kooora.com") -> R.drawable.ic_kooora
            else -> null
        }
    }

    fun saveFavicon(context: Context, url: String, bitmap: Bitmap) {
        val domain = extractDomain(url)
        if (domain.isBlank() || domain == "about:blank") return

        memoryCache.put(domain, bitmap)

        executor.execute {
            try {
                val cacheDir = File(context.cacheDir, "favicons").apply { if (!exists()) mkdirs() }
                val file = File(cacheDir, "${domain.hashCode()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getFavicon(context: Context, url: String): Bitmap? {
        val domain = extractDomain(url)
        if (domain.isBlank() || domain == "about:blank") return null

        val mem = memoryCache.get(domain)
        if (mem != null) return mem

        try {
            val cacheDir = File(context.cacheDir, "favicons")
            val file = File(cacheDir, "${domain.hashCode()}.png")
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    memoryCache.put(domain, bmp)
                    return bmp
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun loadFaviconAsync(context: Context, url: String, onLoaded: (Bitmap) -> Unit) {
        val domain = extractDomain(url)
        if (domain.isBlank() || domain == "about:blank") return

        val mem = memoryCache.get(domain)
        if (mem != null) {
            mainHandler.post { onLoaded(mem) }
            return
        }

        executor.execute {
            try {
                val cacheDir = File(context.cacheDir, "favicons")
                val file = File(cacheDir, "${domain.hashCode()}.png")
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        memoryCache.put(domain, bmp)
                        mainHandler.post { onLoaded(bmp) }
                        return@execute
                    }
                }
            } catch (_: Exception) {}

            val downloadedBmp = fetchFaviconFromNetwork(domain)
            if (downloadedBmp != null) {
                saveFavicon(context, url, downloadedBmp)
                mainHandler.post { onLoaded(downloadedBmp) }
            }
        }
    }

    fun loadFavicon(
        context: Context,
        imageView: ImageView,
        url: String,
        fallbackRes: Int = R.drawable.ic_globe
    ) {
        val domain = extractDomain(url)
        imageView.tag = url

        // 1. Check known built-in vector icons (e.g. Google, YouTube, Facebook, GitHub, etc.)
        val knownRes = getKnownIconRes(url)
        if (knownRes != null) {
            imageView.clearColorFilter()
            imageView.setImageResource(knownRes)
            return
        }

        // 2. Check Memory / Disk Cache
        val cachedBitmap = getFavicon(context, url)
        if (cachedBitmap != null) {
            imageView.clearColorFilter()
            imageView.setImageBitmap(cachedBitmap)
            return
        }

        // 3. Fallback placeholder while fetching
        imageView.setImageResource(fallbackRes)
        imageView.setColorFilter(ContextCompat.getColor(context, R.color.icon_inactive))

        if (domain.isBlank() || domain == "about:blank") return

        // 4. Background fetch from Google Favicon Service / DuckDuckGo
        loadFaviconAsync(context, url) { downloadedBmp ->
            if (imageView.tag == url) {
                imageView.clearColorFilter()
                imageView.setImageBitmap(downloadedBmp)
            }
        }
    }

    private fun fetchFaviconFromNetwork(domain: String): Bitmap? {
        val endpoints = listOf(
            "https://www.google.com/s2/favicons?domain=$domain&sz=128",
            "https://icons.duckduckgo.com/ip3/$domain.ico",
            "https://$domain/favicon.ico"
        )

        for (endpoint in endpoints) {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            try {
                val urlObj = URL(endpoint)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                }
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.inputStream
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    if (bmp != null && bmp.width > 1 && bmp.height > 1) {
                        return bmp
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { connection?.disconnect() } catch (_: Exception) {}
            }
        }
        return null
    }

    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = -0x1
        }
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}
