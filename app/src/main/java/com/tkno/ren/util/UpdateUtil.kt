package com.tkno.ren.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.tkno.ren.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object UpdateUtil {

    private const val OWNER = "hamzabellouch"
    private const val REPO = "ren-browser"
    private const val TAG = "UpdateUtil"

    private fun getReleases(): List<Release> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://api.github.com/repos/$OWNER/$REPO/releases")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Mozilla/5.0 RenBrowser")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }
            if (connection.responseCode !in 200..299) {
                Log.e(TAG, "Releases request failed: HTTP ${connection.responseCode}")
                return emptyList()
            }
            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonText)
            val releases = mutableListOf<Release>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val assetsArray = obj.optJSONArray("assets")
                val assets = mutableListOf<AssetsItem>()
                if (assetsArray != null) {
                    for (j in 0 until assetsArray.length()) {
                        val assetObj = assetsArray.getJSONObject(j)
                        assets.add(
                            AssetsItem(
                                name = assetObj.optString("name").takeIf { it.isNotEmpty() },
                                contentType = assetObj.optString("content_type").takeIf { it.isNotEmpty() },
                                size = if (assetObj.has("size")) assetObj.optLong("size") else null,
                                downloadCount = if (assetObj.has("download_count")) assetObj.optInt("download_count") else null,
                                createdAt = assetObj.optString("created_at").takeIf { it.isNotEmpty() },
                                updatedAt = assetObj.optString("updated_at").takeIf { it.isNotEmpty() },
                                browserDownloadUrl = assetObj.optString("browser_download_url").takeIf { it.isNotEmpty() },
                            )
                        )
                    }
                }
                releases.add(
                    Release(
                        htmlUrl = obj.optString("html_url").takeIf { it.isNotEmpty() },
                        tagName = obj.optString("tag_name").takeIf { it.isNotEmpty() },
                        name = obj.optString("name").takeIf { it.isNotEmpty() },
                        draft = if (obj.has("draft")) obj.optBoolean("draft") else false,
                        preRelease = if (obj.has("prerelease")) obj.optBoolean("prerelease") else false,
                        createdAt = obj.optString("created_at").takeIf { it.isNotEmpty() },
                        publishedAt = obj.optString("published_at").takeIf { it.isNotEmpty() },
                        assets = assets,
                        body = obj.optString("body").takeIf { it.isNotEmpty() },
                    )
                )
            }
            releases
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to fetch releases list", e)
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun checkForUpdate(context: Context, includePrerelease: Boolean = true): Release? =
        withContext(Dispatchers.IO) {
            val currentVersion = context.getCurrentVersion()
            val releases = getReleases()
            if (releases.isEmpty()) return@withContext null

            val candidate = releases
                .filter { release -> release.draft != true }
                .filter { release -> includePrerelease || release.preRelease != true }
                .filter { release -> release.assets?.any { it.name?.endsWith(".apk", ignoreCase = true) == true } == true }
                .maxByOrNull { release -> (release.tagName ?: release.name).toVersion() }
                ?: return@withContext null

            val latestVersion = (candidate.tagName ?: candidate.name).toVersion()
            if (currentVersion < latestVersion) candidate else null
        }

    private fun Context.getCurrentVersion(): Version =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager
                .getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                .versionName
                ?.toVersion() ?: EMPTY_VERSION
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName?.toVersion() ?: EMPTY_VERSION
        }

    private fun Context.getLatestApk() = File(getExternalFilesDir("apk"), "latest.apk")

    fun installLatestApk(context: Context) {
        runCatching {
            val file = context.getLatestApk()
            if (!file.exists() || file.length() == 0L) {
                Log.e(TAG, "APK file does not exist or is empty")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        }.onFailure { Log.e(TAG, "Failed to launch package installer", it) }
    }

    fun openApkLocation(context: Context, release: Release) {
        val prefs = context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE)
        val customUriString = prefs.getString("app_update_directory_uri", null)

        if (!customUriString.isNullOrBlank()) {
            val openedCustom = runCatching {
                val uri = Uri.parse(customUriString)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (openedCustom) return
        }

        val openedDownloads = runCatching {
            val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (openedDownloads) return

        runCatching {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(downloadDir), "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.apk_saved_to_files),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    suspend fun downloadApk(context: Context, release: Release): Flow<DownloadStatus> =
        withContext(Dispatchers.IO) {
            val saveFile = context.getLatestApk()
            val apks = release.assets?.filter { it.name?.endsWith(".apk", ignoreCase = true) == true }
            if (apks.isNullOrEmpty()) return@withContext emptyFlow()

            val abiList = Build.SUPPORTED_ABIS
            val targetAsset = abiList.firstNotNullOfOrNull { abi ->
                apks.find { it.name?.contains(abi, ignoreCase = true) == true }
            } ?: apks.find { it.name?.contains("universal", ignoreCase = true) == true }
            ?: apks.firstOrNull()

            val targetUrl = targetAsset?.browserDownloadUrl ?: return@withContext emptyFlow()

            return@withContext flow {
                emit(DownloadStatus.Progress(0))
                var deleteFile = true
                var connection: HttpURLConnection? = null
                try {
                    var currentUrl = targetUrl
                    var redirectCount = 0
                    while (redirectCount < 5) {
                        val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                            instanceFollowRedirects = true
                            connectTimeout = 30000
                            readTimeout = 30000
                            setRequestProperty("User-Agent", "Mozilla/5.0 RenBrowser")
                        }
                        val status = conn.responseCode
                        if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                            status == HttpURLConnection.HTTP_MOVED_PERM ||
                            status == HttpURLConnection.HTTP_SEE_OTHER ||
                            status == 307 || status == 308
                        ) {
                            val newUrl = conn.getHeaderField("Location")
                            conn.disconnect()
                            if (newUrl != null) {
                                currentUrl = newUrl
                                redirectCount++
                                continue
                            }
                        }
                        connection = conn
                        break
                    }

                    val responseCode = connection?.responseCode ?: -1
                    if (responseCode !in 200..299) {
                        throw Exception("HTTP download failed: $responseCode")
                    }

                    saveFile.parentFile?.mkdirs()
                    val totalBytes = connection?.contentLengthLong ?: -1L
                    connection?.inputStream?.use { input ->
                        saveFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var progressBytes = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                progressBytes += bytesRead
                                val percent = if (totalBytes > 0) {
                                    ((progressBytes * 100) / totalBytes).toInt()
                                } else 50
                                emit(DownloadStatus.Progress(percent = percent))
                            }
                            if (totalBytes > 0 && progressBytes < totalBytes) {
                                throw Exception("Missing bytes: read $progressBytes of $totalBytes")
                            }
                        }
                    }
                    deleteFile = false
                    copyApkToUserDirectory(context, saveFile, release)
                    emit(DownloadStatus.Finished(saveFile))
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e(TAG, "Download error", e)
                    throw e
                } finally {
                    connection?.disconnect()
                    if (deleteFile && saveFile.exists()) {
                        saveFile.delete()
                    }
                }
            }
                .flowOn(Dispatchers.IO)
                .distinctUntilChanged()
        }

    private fun copyApkToUserDirectory(context: Context, sourceFile: File, release: Release) {
        val prefs = context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE)
        val customUriString = prefs.getString("app_update_directory_uri", null)
        val releaseTag = release.tagName ?: release.name ?: "update"
        val apkFileName = "Ren-Browser-$releaseTag.apk"

        if (!customUriString.isNullOrBlank()) {
            runCatching {
                val treeUri = Uri.parse(customUriString)
                val docDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                if (docDir != null && docDir.canWrite()) {
                    docDir.findFile(apkFileName)?.delete()
                    val targetFile = docDir.createFile("application/vnd.android.package-archive", apkFileName)
                    if (targetFile != null) {
                        context.contentResolver.openOutputStream(targetFile.uri)?.use { outStream ->
                            sourceFile.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                    }
                }
            }.onFailure { Log.e(TAG, "Failed to copy APK to custom SAF directory", it) }
        } else {
            runCatching {
                val publicDownloads = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                if (publicDownloads.exists() && publicDownloads.canWrite()) {
                    val targetFile = File(publicDownloads, apkFileName)
                    sourceFile.copyTo(targetFile, overwrite = true)
                }
            }.onFailure { Log.e(TAG, "Failed to copy APK to public Download directory", it) }
        }
    }

    data class Release(
        val htmlUrl: String? = null,
        val tagName: String? = null,
        val name: String? = null,
        val draft: Boolean? = null,
        val preRelease: Boolean? = null,
        val createdAt: String? = null,
        val publishedAt: String? = null,
        val assets: List<AssetsItem>? = null,
        val body: String? = null,
    )

    data class AssetsItem(
        val name: String? = null,
        val contentType: String? = null,
        val size: Long? = null,
        val downloadCount: Int? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
        val browserDownloadUrl: String? = null,
    )

    sealed class DownloadStatus {
        object NotYet : DownloadStatus()
        data class Progress(val percent: Int) : DownloadStatus()
        data class Finished(val file: File) : DownloadStatus()
    }

    private val pattern = Pattern.compile("""v?(\d+)\.(\d+)(?:\.(\d+))?(?:[-.](alpha|beta|rc)(?:[\.-]?(\d+))?)?""", Pattern.CASE_INSENSITIVE)
    private val EMPTY_VERSION = Version.Stable()

    fun String?.toVersion(): Version =
        this?.run {
            val matcher = pattern.matcher(this.trim())
            if (matcher.find()) {
                val major = matcher.group(1)?.toIntOrNull() ?: 0
                val minor = matcher.group(2)?.toIntOrNull() ?: 0
                val patch = matcher.group(3)?.toIntOrNull() ?: 0
                val buildNumber = matcher.group(5)?.toIntOrNull() ?: 0
                val tag = matcher.group(4)?.lowercase()
                when (tag) {
                    "alpha" -> Version.Alpha(major, minor, patch, buildNumber)
                    "beta" -> Version.Beta(major, minor, patch, buildNumber)
                    "rc" -> Version.ReleaseCandidate(major, minor, patch, buildNumber)
                    else -> Version.Stable(major, minor, patch)
                }
            } else EMPTY_VERSION
        } ?: EMPTY_VERSION

    sealed class Version(val major: Int, val minor: Int, val patch: Int, val build: Int = 0) :
        Comparable<Version> {
        companion object {
            private const val BUILD = 10L
            private const val VARIANT = 100L
            private const val PATCH = 10_000L
            private const val MINOR = 1_000_000L
            private const val MAJOR = 100_000_000L

            private const val STABLE = VARIANT * 4
            private const val ALPHA = VARIANT * 1
            private const val BETA = VARIANT * 2
            private const val RELEASE_CANDIDATE = VARIANT * 3
        }

        abstract fun toNumber(): Long

        class Alpha(
            versionMajor: Int = 0, versionMinor: Int = 0, versionPatch: Int = 0, versionBuild: Int = 0
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + ALPHA
        }

        class Beta(
            versionMajor: Int = 0, versionMinor: Int = 0, versionPatch: Int = 0, versionBuild: Int = 0
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + BETA
        }

        class ReleaseCandidate(
            versionMajor: Int = 0, versionMinor: Int = 0, versionPatch: Int = 0, versionBuild: Int = 0
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + RELEASE_CANDIDATE
        }

        class Stable(
            versionMajor: Int = 0, versionMinor: Int = 0, versionPatch: Int = 0
        ) : Version(versionMajor, versionMinor, versionPatch) {
            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + STABLE
        }

        override operator fun compareTo(other: Version): Int =
            this.toNumber().compareTo(other.toNumber())
    }
}
