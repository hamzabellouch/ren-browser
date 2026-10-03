package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Manages fetching, caching, debouncing, and privacy settings for search suggestions
 * across supported search engines (Google, DuckDuckGo, Brave, Qwant, Bing, Ecosia, etc.).
 */
object SearchSuggestionManager {

    const val PREFS_NAME = "ren_browser_search_prefs"
    const val KEY_SEARCH_SUGGESTIONS_ENABLED = "search_suggestions_enabled"

    private const val CONNECT_TIMEOUT_MS = 1500
    private const val READ_TIMEOUT_MS = 1500
    private const val CACHE_MAX_ENTRIES = 150
    private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes

    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private data class CacheEntry(
        val suggestions: List<String>,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Thread-safe LRU Cache with capacity limit
    private val suggestionCache: MutableMap<String, CacheEntry> =
        Collections.synchronizedMap(object : LinkedHashMap<String, CacheEntry>(CACHE_MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
                return size > CACHE_MAX_ENTRIES
            }
        })

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // =========================================================================
    // Privacy & Settings Configuration
    // =========================================================================

    /**
     * Checks if search suggestions are enabled by the user.
     * Defaults to true for convenience, but can be toggled off for strict privacy.
     */
    fun isSearchSuggestionsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SEARCH_SUGGESTIONS_ENABLED, true)
    }

    /**
     * Enables or disables search suggestions.
     */
    fun setSearchSuggestionsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SEARCH_SUGGESTIONS_ENABLED, enabled).apply()
        if (!enabled) {
            clearCache()
        }
    }

    // =========================================================================
    // Search Suggestions Retrieval
    // =========================================================================

    /**
     * Asynchronously fetches search suggestions for a given [query].
     * Checks user setting, cache, and executes network request on [Dispatchers.IO]
     * with a fast timeout (1.5s).
     *
     * @param context Application context
     * @param query The current search query typed by the user
     * @param engineId Optional engine ID ("google", "duckduckgo", "brave", "qwant", etc.).
     *                 If null or empty, uses the selected default search engine.
     * @param maxSuggestions Maximum number of suggestions to return (default: 10)
     */
    suspend fun getSuggestions(
        context: Context,
        query: String,
        engineId: String? = null,
        maxSuggestions: Int = 10
    ): List<String> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext emptyList()
        }

        if (!isSearchSuggestionsEnabled(context)) {
            return@withContext emptyList()
        }

        val resolvedEngineId = if (!engineId.isNullOrBlank()) {
            engineId.lowercase()
        } else {
            SearchEngineManager.getSelectedSearchEngine(context).id.lowercase()
        }

        val cacheKey = "$resolvedEngineId:${trimmedQuery.lowercase()}"
        val cached = suggestionCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            return@withContext cached.suggestions.take(maxSuggestions)
        }

        val suggestions = fetchFromNetwork(resolvedEngineId, trimmedQuery, maxSuggestions)
        if (suggestions.isNotEmpty()) {
            suggestionCache[cacheKey] = CacheEntry(suggestions)
        }
        return@withContext suggestions.take(maxSuggestions)
    }

    /**
     * Debounced search suggestion fetcher for live search inputs.
     */
    suspend fun getSuggestionsWithDebounce(
        context: Context,
        query: String,
        engineId: String? = null,
        debounceMs: Long = 180L,
        maxSuggestions: Int = 10
    ): List<String> {
        if (debounceMs > 0) {
            delay(debounceMs)
        }
        return getSuggestions(context, query, engineId, maxSuggestions)
    }

    /**
     * Synchronous blocking fetcher (suitable for background worker threads).
     */
    fun fetchSuggestionsSync(
        context: Context,
        query: String,
        engineId: String? = null,
        maxSuggestions: Int = 10
    ): List<String> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty() || !isSearchSuggestionsEnabled(context)) {
            return emptyList()
        }

        val resolvedEngineId = if (!engineId.isNullOrBlank()) {
            engineId.lowercase()
        } else {
            SearchEngineManager.getSelectedSearchEngine(context).id.lowercase()
        }

        val cacheKey = "$resolvedEngineId:${trimmedQuery.lowercase()}"
        val cached = suggestionCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            return cached.suggestions.take(maxSuggestions)
        }

        val suggestions = fetchFromNetwork(resolvedEngineId, trimmedQuery, maxSuggestions)
        if (suggestions.isNotEmpty()) {
            suggestionCache[cacheKey] = CacheEntry(suggestions)
        }
        return suggestions.take(maxSuggestions)
    }

    /**
     * Clears in-memory suggestions cache.
     */
    fun clearCache() {
        suggestionCache.clear()
    }

    // =========================================================================
    // Network Fetching & Parsing
    // =========================================================================

    private fun fetchFromNetwork(engineId: String, query: String, maxSuggestions: Int): List<String> {
        val suggestionUrl = buildSuggestionUrl(engineId, query) ?: return emptyList()
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(suggestionUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("Accept-Charset", "UTF-8")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return emptyList()
            }

            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            parseSuggestions(engineId, responseBody, maxSuggestions)
        } catch (e: Exception) {
            // Protect against network issues, DNS errors, or timeouts without crashing
            emptyList()
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) {
                // Ignore disconnect errors
            }
        }
    }

    /**
     * Builds the appropriate suggestion endpoint URL for the specified search engine.
     */
    fun buildSuggestionUrl(engineId: String, query: String): String? {
        val encodedQuery = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query
        }

        return when (engineId.lowercase()) {
            "google" -> "https://suggestqueries.google.com/complete/search?client=chrome&q=$encodedQuery"
            "duckduckgo" -> "https://duckduckgo.com/ac/?q=$encodedQuery"
            "brave" -> "https://search.brave.com/api/suggest?q=$encodedQuery"
            "qwant" -> "https://api.qwant.com/v3/suggest?q=$encodedQuery"
            "bing" -> "https://api.bing.com/osjson.aspx?query=$encodedQuery"
            "ecosia" -> "https://ac.ecosia.org/autocomplete?q=$encodedQuery"
            "yahoo" -> "https://search.yahoo.com/sugg/gflx?output=json&command=$encodedQuery"
            "startpage" -> "https://startpage.com/suggestions?q=$encodedQuery"
            "baidu" -> "https://suggestion.baidu.com/su?wd=$encodedQuery"
            "yandex" -> "https://suggest.yandex.com/suggest-ya.cgi?part=$encodedQuery"
            else -> "https://suggestqueries.google.com/complete/search?client=chrome&q=$encodedQuery"
        }
    }

    /**
     * Parses the engine-specific JSON response into a list of suggestion strings.
     */
    fun parseSuggestions(engineId: String, jsonStr: String, maxSuggestions: Int = 10): List<String> {
        if (jsonStr.isBlank()) return emptyList()

        val results = mutableListOf<String>()
        try {
            when (engineId.lowercase()) {
                "google", "bing", "ecosia", "startpage" -> {
                    // OpenSearch format: ["query", ["sug1", "sug2", ...]]
                    parseOpenSearchFormat(jsonStr, results)
                }
                "duckduckgo" -> {
                    // DuckDuckGo format: [{"phrase": "suggestion1"}, {"phrase": "suggestion2"}]
                    parseDuckDuckGoFormat(jsonStr, results)
                }
                "brave" -> {
                    // Brave format: ["query", ["sug1", ...]] or {"query": "...", "results": [{"title": "sug1"}]}
                    val trimmed = jsonStr.trim()
                    if (trimmed.startsWith("[")) {
                        parseOpenSearchFormat(trimmed, results)
                    } else if (trimmed.startsWith("{")) {
                        val obj = JSONObject(trimmed)
                        val itemsArray = obj.optJSONArray("results")
                        if (itemsArray != null) {
                            for (i in 0 until itemsArray.length()) {
                                val item = itemsArray.optJSONObject(i) ?: continue
                                val text = item.optString("title").ifBlank { item.optString("query") }
                                if (text.isNotBlank()) results.add(text)
                            }
                        }
                    }
                }
                "qwant" -> {
                    // Qwant format: {"status":"success","data":{"items":[{"value":"suggestion1"}]}} or OpenSearch array
                    val trimmed = jsonStr.trim()
                    if (trimmed.startsWith("[")) {
                        parseOpenSearchFormat(trimmed, results)
                    } else if (trimmed.startsWith("{")) {
                        val obj = JSONObject(trimmed)
                        val dataObj = obj.optJSONObject("data")
                        val itemsArray = dataObj?.optJSONArray("items") ?: obj.optJSONArray("items")
                        if (itemsArray != null) {
                            for (i in 0 until itemsArray.length()) {
                                val item = itemsArray.optJSONObject(i) ?: continue
                                val text = item.optString("value")
                                    .ifBlank { item.optString("suggest") }
                                    .ifBlank { item.optString("title") }
                                if (text.isNotBlank()) results.add(text)
                            }
                        }
                    }
                }
                "yahoo" -> {
                    // Yahoo format: {"gflx": {"r": [{"k": "sug1"}, ...]}} or OpenSearch
                    val trimmed = jsonStr.trim()
                    if (trimmed.startsWith("[")) {
                        parseOpenSearchFormat(trimmed, results)
                    } else if (trimmed.startsWith("{")) {
                        val obj = JSONObject(trimmed)
                        val gflx = obj.optJSONObject("gflx")
                        val rArray = gflx?.optJSONArray("r")
                        if (rArray != null) {
                            for (i in 0 until rArray.length()) {
                                val item = rArray.optJSONObject(i) ?: continue
                                val text = item.optString("k")
                                if (text.isNotBlank()) results.add(text)
                            }
                        }
                    }
                }
                else -> {
                    // Generic fallback: Try OpenSearch format first, then DuckDuckGo object array format
                    val trimmed = jsonStr.trim()
                    if (trimmed.startsWith("[")) {
                        try {
                            parseOpenSearchFormat(trimmed, results)
                        } catch (e: Exception) {
                            parseDuckDuckGoFormat(trimmed, results)
                        }
                    } else if (trimmed.startsWith("{")) {
                        val obj = JSONObject(trimmed)
                        val dataArray = obj.optJSONArray("results") ?: obj.optJSONArray("items") ?: obj.optJSONArray("suggestions")
                        if (dataArray != null) {
                            for (i in 0 until dataArray.length()) {
                                val itemStr = dataArray.optString(i)
                                if (itemStr.isNotBlank()) {
                                    results.add(itemStr)
                                } else {
                                    val itemObj = dataArray.optJSONObject(i)
                                    val text = itemObj?.optString("phrase")
                                        ?.ifBlank { itemObj.optString("title") }
                                        ?.ifBlank { itemObj.optString("value") }
                                    if (!text.isNullOrBlank()) results.add(text)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Protect against unexpected format changes
        }

        // Deduplicate and filter empty
        return results
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(maxSuggestions)
    }

    private fun parseOpenSearchFormat(jsonStr: String, results: MutableList<String>) {
        val array = JSONArray(jsonStr)
        if (array.length() > 1) {
            val suggestionsArray = array.optJSONArray(1)
            if (suggestionsArray != null) {
                for (i in 0 until suggestionsArray.length()) {
                    val sug = suggestionsArray.optString(i, "")
                    if (sug.isNotBlank()) {
                        results.add(sug)
                    }
                }
            }
        }
    }

    private fun parseDuckDuckGoFormat(jsonStr: String, results: MutableList<String>) {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val phrase = obj.optString("phrase", "")
            if (phrase.isNotBlank()) {
                results.add(phrase)
            }
        }
    }
}
