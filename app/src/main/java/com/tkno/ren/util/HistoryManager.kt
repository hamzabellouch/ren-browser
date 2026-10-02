package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

object HistoryManager {

    private const val PREFS_NAME = "ren_browser_history_prefs"
    private const val KEY_HISTORY_JSON = "browser_history_json"
    private const val MAX_HISTORY_ITEMS = 200

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val jsonStr = getPrefs(context).getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        val list = mutableListOf<HistoryItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "Untitled")
                val url = obj.optString("url", "")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                if (url.isNotBlank() && url != "about:blank") {
                    list.add(HistoryItem(id, title, url, timestamp))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun addHistory(context: Context, title: String, url: String) {
        if (url.isBlank() || url == "about:blank") return

        val displayTitle = if (title.isBlank() || title == "Blank page") {
            try {
                val uri = android.net.Uri.parse(url)
                uri.host ?: url
            } catch (e: Exception) {
                url
            }
        } else {
            title
        }

        val currentList = getHistory(context).toMutableList()

        // Remove duplicates of same URL if visited recently, so it moves to top
        currentList.removeAll { it.url.equals(url, ignoreCase = true) }

        currentList.add(0, HistoryItem(
            id = UUID.randomUUID().toString(),
            title = displayTitle,
            url = url,
            timestamp = System.currentTimeMillis()
        ))

        // Keep list within max items
        val trimmed = if (currentList.size > MAX_HISTORY_ITEMS) {
            currentList.subList(0, MAX_HISTORY_ITEMS)
        } else {
            currentList
        }

        saveHistory(context, trimmed)
    }

    fun deleteHistoryItem(context: Context, id: String) {
        val currentList = getHistory(context).toMutableList()
        currentList.removeAll { it.id == id }
        saveHistory(context, currentList)
    }

    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY_JSON).apply()
    }

    /* ---------------- Isolated In-Memory Incognito History ---------------- */
    private val incognitoHistoryList = mutableListOf<HistoryItem>()

    @Synchronized
    fun getIncognitoHistory(): List<HistoryItem> {
        return incognitoHistoryList.toList().sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun addIncognitoHistory(title: String, url: String) {
        if (url.isBlank() || url == "about:blank") return

        val displayTitle = if (title.isBlank() || title == "Blank page" || title == "Incognito") {
            try {
                val uri = android.net.Uri.parse(url)
                uri.host ?: url
            } catch (e: Exception) {
                url
            }
        } else {
            title
        }

        incognitoHistoryList.removeAll { it.url.equals(url, ignoreCase = true) }
        incognitoHistoryList.add(0, HistoryItem(
            id = UUID.randomUUID().toString(),
            title = displayTitle,
            url = url,
            timestamp = System.currentTimeMillis()
        ))

        if (incognitoHistoryList.size > MAX_HISTORY_ITEMS) {
            incognitoHistoryList.removeAt(incognitoHistoryList.size - 1)
        }
    }

    @Synchronized
    fun deleteIncognitoHistoryItem(id: String) {
        incognitoHistoryList.removeAll { it.id == id }
    }

    @Synchronized
    fun clearIncognitoHistory() {
        incognitoHistoryList.clear()
    }

    private fun saveHistory(context: Context, items: List<HistoryItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_HISTORY_JSON, jsonArray.toString()).apply()
    }
}
