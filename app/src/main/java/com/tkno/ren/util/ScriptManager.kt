package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

data class UserScript(
    val id: String,
    var name: String,
    var description: String = "",
    var version: String = "1.0",
    var author: String = "",
    var matchPatterns: List<String> = listOf("*"),
    var runAt: String = "document-end", // "document-start", "document-end", "document-idle"
    var isEnabled: Boolean = true,
    var sourceUrl: String? = null,
    var lastUpdatedTime: Long = System.currentTimeMillis()
) {
    fun getCode(context: Context): String {
        return ScriptManager.getScriptCode(context, id)
    }
}

object ScriptManager {

    private const val PREFS_NAME = "ren_scripts_prefs"
    private const val KEY_SCRIPTS_ENABLED = "scripts_enabled"
    private const val KEY_SCRIPTS_JSON = "scripts_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isScriptsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SCRIPTS_ENABLED, true)
    }

    fun setScriptsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SCRIPTS_ENABLED, enabled).apply()
    }

    private fun getScriptsDir(context: Context): File {
        val dir = File(context.filesDir, "user_scripts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getScriptCode(context: Context, scriptId: String): String {
        val file = File(getScriptsDir(context), "$scriptId.js")
        return if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    fun saveScriptCode(context: Context, scriptId: String, code: String) {
        try {
            val file = File(getScriptsDir(context), "$scriptId.js")
            file.writeText(code)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getScripts(context: Context): List<UserScript> {
        val jsonStr = getPrefs(context).getString(KEY_SCRIPTS_JSON, null) ?: return emptyList()
        val list = mutableListOf<UserScript>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val matchPatterns = mutableListOf<String>()
                val matchesArr = obj.optJSONArray("matchPatterns")
                if (matchesArr != null) {
                    for (j in 0 until matchesArr.length()) {
                        matchPatterns.add(matchesArr.getString(j))
                    }
                }
                if (matchPatterns.isEmpty()) {
                    matchPatterns.add("*")
                }

                list.add(
                    UserScript(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Unnamed Script"),
                        description = obj.optString("description", ""),
                        version = obj.optString("version", "1.0"),
                        author = obj.optString("author", ""),
                        matchPatterns = matchPatterns,
                        runAt = obj.optString("runAt", "document-end"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        sourceUrl = if (obj.has("sourceUrl")) obj.optString("sourceUrl") else null,
                        lastUpdatedTime = obj.optLong("lastUpdatedTime", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveScripts(context: Context, list: List<UserScript>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("description", item.description)
                put("version", item.version)
                put("author", item.author)
                val matchesArr = JSONArray()
                for (p in item.matchPatterns) {
                    matchesArr.put(p)
                }
                put("matchPatterns", matchesArr)
                put("runAt", item.runAt)
                put("isEnabled", item.isEnabled)
                if (item.sourceUrl != null) put("sourceUrl", item.sourceUrl)
                put("lastUpdatedTime", item.lastUpdatedTime)
            }
            arr.put(obj)
        }
        getPrefs(context).edit().putString(KEY_SCRIPTS_JSON, arr.toString()).apply()
    }

    fun toggleScript(context: Context, id: String, isEnabled: Boolean) {
        val list = getScripts(context).toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            list[index].isEnabled = isEnabled
            saveScripts(context, list)
        }
    }

    fun deleteScript(context: Context, id: String) {
        val list = getScripts(context).filterNot { it.id == id }
        saveScripts(context, list)
        try {
            val file = File(getScriptsDir(context), "$id.js")
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    // Parses standard Greasemonkey / Tampermonkey header metadata from script code
    fun parseScriptMetadata(code: String): UserScript {
        var name = "Custom Script"
        var description = ""
        var version = "1.0"
        var author = ""
        var sourceUrl: String? = null
        val matchPatterns = mutableListOf<String>()
        var runAt = "document-end"

        val lines = code.lines()
        var inMeta = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("==UserScript==", ignoreCase = true)) {
                inMeta = true
                continue
            }
            if (trimmed.contains("==/UserScript==", ignoreCase = true)) {
                inMeta = false
                break
            }
            if (inMeta && trimmed.startsWith("//")) {
                val content = trimmed.substring(2).trim()
                if (content.startsWith("@name", ignoreCase = true)) {
                    name = content.substring(5).trim().ifBlank { name }
                } else if (content.startsWith("@description", ignoreCase = true)) {
                    description = content.substring(12).trim()
                } else if (content.startsWith("@version", ignoreCase = true)) {
                    version = content.substring(8).trim().ifBlank { version }
                } else if (content.startsWith("@author", ignoreCase = true)) {
                    author = content.substring(7).trim()
                } else if (content.startsWith("@match", ignoreCase = true)) {
                    val pattern = content.substring(6).trim()
                    if (pattern.isNotEmpty()) matchPatterns.add(pattern)
                } else if (content.startsWith("@include", ignoreCase = true)) {
                    val pattern = content.substring(8).trim()
                    if (pattern.isNotEmpty()) matchPatterns.add(pattern)
                } else if (content.startsWith("@updateURL", ignoreCase = true)) {
                    val u = content.substring(10).trim()
                    if (u.isNotEmpty()) sourceUrl = u
                } else if (content.startsWith("@downloadURL", ignoreCase = true)) {
                    val u = content.substring(12).trim()
                    if (u.isNotEmpty() && sourceUrl == null) sourceUrl = u
                } else if (content.startsWith("@run-at", ignoreCase = true)) {
                    val r = content.substring(7).trim().lowercase(Locale.ROOT)
                    if (r.contains("start")) {
                        runAt = "document-start"
                    } else if (r.contains("idle")) {
                        runAt = "document-idle"
                    } else {
                        runAt = "document-end"
                    }
                }
            }
        }

        if (matchPatterns.isEmpty()) {
            matchPatterns.add("*")
        }

        return UserScript(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            version = version,
            author = author,
            matchPatterns = matchPatterns,
            runAt = runAt,
            isEnabled = true,
            sourceUrl = sourceUrl,
            lastUpdatedTime = System.currentTimeMillis()
        )
    }

    fun addScript(context: Context, code: String, sourceUrl: String? = null): UserScript {
        val script = parseScriptMetadata(code)
        if (sourceUrl != null) {
            script.sourceUrl = sourceUrl
        }
        saveScriptCode(context, script.id, code)

        val list = getScripts(context).toMutableList()
        val existingIndex = list.indexOfFirst { it.name.equals(script.name, ignoreCase = true) || (script.sourceUrl != null && it.sourceUrl == script.sourceUrl) }
        if (existingIndex >= 0) {
            // Update existing script
            val existing = list[existingIndex]
            existing.name = script.name
            existing.description = script.description
            existing.version = script.version
            existing.author = script.author
            existing.matchPatterns = script.matchPatterns
            existing.runAt = script.runAt
            existing.lastUpdatedTime = System.currentTimeMillis()
            if (script.sourceUrl != null) existing.sourceUrl = script.sourceUrl
            saveScriptCode(context, existing.id, code)
            saveScripts(context, list)
            return existing
        } else {
            list.add(script)
            saveScripts(context, list)
            return script
        }
    }

    fun updateScript(
        context: Context,
        script: UserScript,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val source = script.sourceUrl
        if (source.isNullOrBlank()) {
            onComplete(false, "No source URL for this script")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var conn: HttpURLConnection? = null
            var updated = false
            var message = ""
            try {
                val cleanUrl = source.trim()
                val normalizedUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                    "https://$cleanUrl"
                } else {
                    cleanUrl
                }
                val url = URL(normalizedUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "RenBrowser/1.0")

                if (conn.responseCode in 200..299) {
                    val newCode = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    if (newCode.isNotBlank()) {
                        val parsed = parseScriptMetadata(newCode)
                        val list = getScripts(context).toMutableList()
                        val idx = list.indexOfFirst { it.id == script.id }
                        if (idx >= 0) {
                            val target = list[idx]
                            target.name = parsed.name
                            target.description = parsed.description
                            target.version = parsed.version
                            target.author = parsed.author
                            target.matchPatterns = parsed.matchPatterns
                            target.runAt = parsed.runAt
                            target.lastUpdatedTime = System.currentTimeMillis()
                            saveScripts(context, list)
                            saveScriptCode(context, target.id, newCode)
                            updated = true
                            message = "Updated \"${target.name}\" to v${target.version}"
                        }
                    } else {
                        message = "Downloaded script is empty"
                    }
                } else {
                    message = "Server returned HTTP ${conn.responseCode}"
                }
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Failed to update script"
            } finally {
                conn?.disconnect()
            }
            withContext(Dispatchers.Main) {
                onComplete(updated, message)
            }
        }
    }

    fun updateAllScripts(
        context: Context,
        onComplete: (updatedCount: Int, totalOnlineCount: Int) -> Unit
    ) {
        val scriptsWithSource = getScripts(context).filter { !it.sourceUrl.isNullOrBlank() }
        if (scriptsWithSource.isEmpty()) {
            onComplete(0, 0)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var updatedCount = 0
            for (script in scriptsWithSource) {
                val source = script.sourceUrl ?: continue
                var conn: HttpURLConnection? = null
                try {
                    val cleanUrl = source.trim()
                    val normalizedUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                        "https://$cleanUrl"
                    } else {
                        cleanUrl
                    }
                    val url = URL(normalizedUrl)
                    conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 15000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "RenBrowser/1.0")

                    if (conn.responseCode in 200..299) {
                        val newCode = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                        if (newCode.isNotBlank()) {
                            val parsed = parseScriptMetadata(newCode)
                            val list = getScripts(context).toMutableList()
                            val idx = list.indexOfFirst { it.id == script.id }
                            if (idx >= 0) {
                                val target = list[idx]
                                target.name = parsed.name
                                target.description = parsed.description
                                target.version = parsed.version
                                target.author = parsed.author
                                target.matchPatterns = parsed.matchPatterns
                                target.runAt = parsed.runAt
                                target.lastUpdatedTime = System.currentTimeMillis()
                                saveScripts(context, list)
                                saveScriptCode(context, target.id, newCode)
                                updatedCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    conn?.disconnect()
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(updatedCount, scriptsWithSource.size)
            }
        }
    }

    fun downloadScriptFromUrl(
        context: Context,
        urlString: String,
        onComplete: (UserScript?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var script: UserScript? = null
            var conn: HttpURLConnection? = null
            try {
                val cleanUrl = urlString.trim()
                val normalizedUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                    "https://$cleanUrl"
                } else {
                    cleanUrl
                }
                val url = URL(normalizedUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "RenBrowser/1.0")

                if (conn.responseCode in 200..299) {
                    val code = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    if (code.isNotBlank()) {
                        script = addScript(context, code, normalizedUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }
            withContext(Dispatchers.Main) {
                onComplete(script)
            }
        }
    }

    // Matches a URL against Greasemonkey/Tampermonkey pattern
    fun matchesUrl(pattern: String, url: String): Boolean {
        if (pattern == "*" || pattern == "*://*/*" || pattern == "<all_urls>") return true

        try {
            // Convert glob pattern to regular expression
            val regex = buildString {
                append("^")
                var i = 0
                while (i < pattern.length) {
                    when (val c = pattern[i]) {
                        '*' -> append(".*")
                        '?' -> append(".")
                        '.', '(', ')', '+', '|', '^', '$', '@', '%', '[', ']', '{', '}' -> {
                            append("\\").append(c)
                        }
                        '\\' -> {
                            if (i + 1 < pattern.length) {
                                append("\\").append(pattern[i + 1])
                                i++
                            } else {
                                append("\\\\")
                            }
                        }
                        else -> append(c)
                    }
                    i++
                }
                append("$")
            }
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(url).matches()
        } catch (e: Exception) {
            return url.contains(pattern.replace("*", ""), ignoreCase = true)
        }
    }

    // Returns matching active scripts for the given URL and execution stage
    fun getMatchingScripts(context: Context, url: String, stage: String): List<UserScript> {
        if (!isScriptsEnabled(context)) return emptyList()
        if (url.startsWith("about:") || url.startsWith("chrome:") || url.startsWith("file:")) return emptyList()

        val allScripts = getScripts(context).filter { it.isEnabled }
        val matching = mutableListOf<UserScript>()

        for (script in allScripts) {
            val scriptStage = script.runAt.lowercase(Locale.ROOT)
            val stageMatch = when (stage) {
                "document-start" -> scriptStage == "document-start"
                "document-end" -> scriptStage == "document-end" || scriptStage == "document-idle"
                else -> true
            }

            if (stageMatch) {
                val matches = script.matchPatterns.any { pattern ->
                    matchesUrl(pattern.trim(), url)
                }
                if (matches) {
                    matching.add(script)
                }
            }
        }
        return matching
    }
}
