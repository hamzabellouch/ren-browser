package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import com.tkno.ren.R
import java.net.URLEncoder

data class SearchEngine(
    val id: String,
    val name: String,
    val searchUrl: String,
    val homeUrl: String,
    val isCustom: Boolean = false
)

object SearchEngineManager {

    const val PREFS_NAME = "ren_browser_search_prefs"
    const val KEY_SELECTED_ENGINE_ID = "selected_search_engine_id"
    private const val KEY_CUSTOM_ENGINE_NAME = "custom_search_engine_name"
    private const val KEY_CUSTOM_ENGINE_URL = "custom_search_engine_url"

    val DEFAULT_SEARCH_ENGINES = listOf(
        SearchEngine(
            id = "google",
            name = "Google",
            searchUrl = "https://www.google.com/search?q=",
            homeUrl = "https://www.google.com"
        ),
        SearchEngine(
            id = "brave",
            name = "Brave Search",
            searchUrl = "https://search.brave.com/search?q=",
            homeUrl = "https://search.brave.com"
        ),
        SearchEngine(
            id = "duckduckgo",
            name = "DuckDuckGo",
            searchUrl = "https://duckduckgo.com/?q=",
            homeUrl = "https://duckduckgo.com"
        ),
        SearchEngine(
            id = "bing",
            name = "Bing",
            searchUrl = "https://www.bing.com/search?q=",
            homeUrl = "https://www.bing.com"
        ),
        SearchEngine(
            id = "ecosia",
            name = "Ecosia",
            searchUrl = "https://www.ecosia.org/search?q=",
            homeUrl = "https://www.ecosia.org"
        ),
        SearchEngine(
            id = "yahoo",
            name = "Yahoo",
            searchUrl = "https://search.yahoo.com/search?p=",
            homeUrl = "https://search.yahoo.com"
        ),
        SearchEngine(
            id = "startpage",
            name = "Startpage",
            searchUrl = "https://www.startpage.com/sp/search?query=",
            homeUrl = "https://www.startpage.com"
        ),
        SearchEngine(
            id = "qwant",
            name = "Qwant",
            searchUrl = "https://www.qwant.com/?q=",
            homeUrl = "https://www.qwant.com"
        ),
        SearchEngine(
            id = "yandex",
            name = "Yandex",
            searchUrl = "https://yandex.com/search/?text=",
            homeUrl = "https://yandex.com"
        ),
        SearchEngine(
            id = "baidu",
            name = "Baidu",
            searchUrl = "https://www.baidu.com/s?wd=",
            homeUrl = "https://www.baidu.com"
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAllSearchEngines(context: Context): List<SearchEngine> {
        val list = DEFAULT_SEARCH_ENGINES.toMutableList()
        val customName = getPrefs(context).getString(KEY_CUSTOM_ENGINE_NAME, null)
        val customUrl = getPrefs(context).getString(KEY_CUSTOM_ENGINE_URL, null)
        if (!customName.isNullOrBlank() && !customUrl.isNullOrBlank()) {
            list.add(
                SearchEngine(
                    id = "custom",
                    name = customName,
                    searchUrl = customUrl,
                    homeUrl = customUrl,
                    isCustom = true
                )
            )
        }
        return list
    }

    fun getSelectedSearchEngine(context: Context): SearchEngine {
        val prefs = getPrefs(context)
        val selectedId = prefs.getString(KEY_SELECTED_ENGINE_ID, "google") ?: "google"
        val all = getAllSearchEngines(context)
        return all.find { it.id == selectedId } ?: DEFAULT_SEARCH_ENGINES.first()
    }

    fun setSelectedSearchEngine(context: Context, engineId: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_ENGINE_ID, engineId).apply()
    }

    fun setCustomSearchEngine(context: Context, name: String, searchUrl: String) {
        val finalUrl = if (!searchUrl.startsWith("http://") && !searchUrl.startsWith("https://")) {
            "https://$searchUrl"
        } else {
            searchUrl
        }
        getPrefs(context).edit()
            .putString(KEY_CUSTOM_ENGINE_NAME, name.trim())
            .putString(KEY_CUSTOM_ENGINE_URL, finalUrl.trim())
            .putString(KEY_SELECTED_ENGINE_ID, "custom")
            .apply()
    }

    fun buildSearchUrl(context: Context, query: String): String {
        val engine = getSelectedSearchEngine(context)
        val encodedQuery = try {
            URLEncoder.encode(query.trim(), "UTF-8")
        } catch (e: Exception) {
            query.trim()
        }

        return if (engine.searchUrl.contains("%s")) {
            engine.searchUrl.replace("%s", encodedQuery)
        } else {
            engine.searchUrl + encodedQuery
        }
    }

    fun getSearchEngineIconRes(engineId: String): Int {
        return when (engineId.lowercase()) {
            "google" -> R.drawable.ic_google_g
            "brave" -> R.drawable.ic_brave
            "duckduckgo" -> R.drawable.ic_duckduckgo
            "bing" -> R.drawable.ic_bing
            "ecosia" -> R.drawable.ic_ecosia
            "yahoo" -> R.drawable.ic_yahoo
            "startpage" -> R.drawable.ic_startpage
            "qwant" -> R.drawable.ic_qwant
            "yandex" -> R.drawable.ic_yandex
            "baidu" -> R.drawable.ic_baidu
            else -> R.drawable.ic_search
        }
    }
}

