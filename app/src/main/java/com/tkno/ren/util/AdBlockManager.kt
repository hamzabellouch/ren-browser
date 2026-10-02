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
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FilterSubscription(
    val id: String,
    val title: String,
    val url: String,
    var filterCount: Int = 0,
    var lastUpdatedTime: Long = System.currentTimeMillis() - 86400000L, // default: Yesterday
    var isEnabled: Boolean = true,
    val isCustom: Boolean = false
) {
    fun getFormattedUpdate(): String {
        val diff = System.currentTimeMillis() - lastUpdatedTime
        return when {
            diff < 60_000L -> "updated just now"
            diff < 3600_000L -> {
                val mins = (diff / 60_000L).coerceAtLeast(1)
                "updated $mins minute${if (mins > 1) "s" else ""} ago"
            }
            diff < 86400_000L -> "updated Today"
            diff < 172800_000L -> "updated Yesterday"
            else -> {
                val sdf = SimpleDateFormat("MMM d", Locale.ENGLISH)
                "updated on " + sdf.format(Date(lastUpdatedTime))
            }
        }
    }
}

object AdBlockManager {

    private const val PREFS_NAME = "ren_adblock_prefs"
    private const val KEY_ADBLOCK_ENABLED = "adblock_enabled"
    private const val KEY_ANTI_ADBLOCK_ENABLED = "anti_adblock_enabled"
    private const val KEY_BLOCKED_ADS_COUNT = "blocked_ads_count"
    private const val KEY_SAVED_DATA_BYTES = "saved_data_bytes"
    private const val KEY_CUSTOM_FILTERS_TEXT = "custom_filters_text"
    private const val KEY_UPDATE_INTERVAL = "update_interval"
    private const val KEY_SUBSCRIPTIONS_JSON = "subscriptions_json"

    // Default presets matching the reference UI
    private val DEFAULT_PRESETS = listOf(
        FilterSubscription(
            id = "easylist",
            title = "EasyList",
            url = "https://easylist.to/easylist/easylist.txt",
            filterCount = 78676,
            lastUpdatedTime = System.currentTimeMillis() - 86400000L,
            isEnabled = true,
            isCustom = false
        ),
        FilterSubscription(
            id = "easyprivacy",
            title = "EasyPrivacy",
            url = "https://easylist.to/easylist/easyprivacy.txt",
            filterCount = 54120,
            lastUpdatedTime = System.currentTimeMillis() - 86400000L,
            isEnabled = true,
            isCustom = false
        ),
        FilterSubscription(
            id = "anti_adblock",
            title = "Adblock Warning Removal List",
            url = "https://easylist-downloads.adblockplus.org/antiadblockfilters.txt",
            filterCount = 3026,
            lastUpdatedTime = System.currentTimeMillis() - 86400000L,
            isEnabled = true,
            isCustom = false
        ),
        FilterSubscription(
            id = "adguard_base",
            title = "raw.githubusercontent.../filter_2_Base/filter.txt",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_2_Base/filter.txt",
            filterCount = 161822,
            lastUpdatedTime = System.currentTimeMillis() - 86400000L,
            isEnabled = true,
            isCustom = false
        ),
        FilterSubscription(
            id = "adguard_spyware",
            title = "raw.githubusercontent...er_3_Spyware/filter.txt",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_3_Spyware/filter.txt",
            filterCount = 331872,
            lastUpdatedTime = System.currentTimeMillis() - 86400000L,
            isEnabled = false,
            isCustom = false
        ),
        FilterSubscription(
            id = "adguard_trackparam",
            title = "raw.githubusercontent...7_TrackParam/filter.txt",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_7_TrackParam/filter.txt",
            filterCount = 3954,
            lastUpdatedTime = System.currentTimeMillis() - 86400000L,
            isEnabled = true,
            isCustom = false
        ),
        FilterSubscription(
            id = "adguard_experimental",
            title = "raw.githubusercontent....Experimental/filter.txt",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_1_Experimental/filter.txt",
            filterCount = 265,
            lastUpdatedTime = System.currentTimeMillis() - 86400000L,
            isEnabled = true,
            isCustom = false
        ),
        FilterSubscription(
            id = "adguard_dns",
            title = "AdGuard DNS filter",
            url = "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt",
            filterCount = 45120,
            lastUpdatedTime = System.currentTimeMillis() - 86400000L,
            isEnabled = true,
            isCustom = false
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAdBlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ADBLOCK_ENABLED, true)
    }

    fun setAdBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ADBLOCK_ENABLED, enabled).apply()
    }

    fun isAntiAdblockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ANTI_ADBLOCK_ENABLED, true)
    }

    fun setAntiAdblockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ANTI_ADBLOCK_ENABLED, enabled).apply()
    }

    fun isBaitScriptRequest(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        return lower.endsWith(".js") ||
               lower.contains(".js?") ||
               lower.contains("/ads.") ||
               lower.contains("/pagead") ||
               lower.contains("/adframe") ||
               lower.contains("/show_ads") ||
               lower.contains("/prebid") ||
               lower.contains("adsbygoogle") ||
               lower.contains("adblock") ||
               lower.contains("sponsor") ||
               lower.contains("advert")
    }

    fun getBaitScriptResponse(): String {
        return """
            (function() {
                try {
                    window.canRunAds = true;
                    window.isAdBlocked = false;
                    window.adblock = false;
                    window.google_ad_client = true;
                    window.adsbygoogle = window.adsbygoogle || [];
                    window.adsbygoogle.loaded = true;
                    window.adsbygoogle.push = function() { return 1; };
                    window.ga = window.ga || function() {};
                    window.gtag = window.gtag || function() {};
                    window.AdBlocker = { isDetected: false };
                    var noop = {
                        check: function() { return true; },
                        on: function(flag, fn) { if (!flag && typeof fn === 'function') { try { fn(); } catch(e) {} } return this; },
                        onDetected: function() { return this; },
                        onNotDetected: function(fn) { if (typeof fn === 'function') { try { fn(); } catch(e) {} } return this; },
                        setOption: function() { return this; }
                    };
                    window.BlockAdBlock = function() { return noop; };
                    window.blockAdBlock = noop;
                    window.FuckAdBlock = function() { return noop; };
                    window.fuckAdBlock = noop;
                    window.SnackAdBlock = noop;
                } catch(e) {}
            })();
        """.trimIndent()
    }

    fun getAntiAdblockScript(): String {
        return """
            (function() {
                try {
                    window.canRunAds = true;
                    window.isAdBlocked = false;
                    window.adblock = false;
                    window.google_ad_client = true;
                    if (!window.adsbygoogle) {
                        window.adsbygoogle = [];
                        window.adsbygoogle.loaded = true;
                        window.adsbygoogle.push = function() { return 1; };
                    }
                    if (!window.ga) window.ga = function() {};
                    if (!window.gtag) window.gtag = function() {};

                    var noop = {
                        check: function() { return true; },
                        on: function(flag, fn) { if (!flag && typeof fn === 'function') { try { fn(); } catch(e) {} } return this; },
                        onDetected: function() { return this; },
                        onNotDetected: function(fn) { if (typeof fn === 'function') { try { fn(); } catch(e) {} } return this; },
                        setOption: function() { return this; }
                    };
                    window.BlockAdBlock = function() { return noop; };
                    window.blockAdBlock = noop;
                    window.FuckAdBlock = function() { return noop; };
                    window.fuckAdBlock = noop;
                    window.SnackAdBlock = noop;

                    var restoreScrollAndCleanModals = function() {
                        try {
                            if (document.documentElement) {
                                document.documentElement.style.setProperty('overflow', 'auto', 'important');
                            }
                            if (document.body) {
                                document.body.style.setProperty('overflow', 'auto', 'important');
                            }
                            var selectors = [
                                '#adblock-overlay', '.adblock-overlay',
                                '#adblock-modal', '.adblock-modal',
                                '.adblock-backdrop', '#adblock-backdrop',
                                '#anti-adblock', '.anti-adblock',
                                '#adblock-blocker', '.adblock-blocker',
                                'div[class*="adblock-notice"]',
                                'div[id*="adblock-notice"]',
                                'div[class*="adblock_warning"]',
                                'div[id*="adblock_warning"]',
                                'div[class*="anti-adblock"]',
                                'div[id*="anti-adblock"]',
                                'div[class*="adblock-dialog"]',
                                'div[id*="adblock-dialog"]'
                            ];
                            var found = document.querySelectorAll(selectors.join(','));
                            for (var i = 0; i < found.length; i++) {
                                var el = found[i];
                                if (el && el.parentNode) {
                                    el.style.setProperty('display', 'none', 'important');
                                    el.style.setProperty('visibility', 'hidden', 'important');
                                }
                            }
                        } catch(e) {}
                    };

                    if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', restoreScrollAndCleanModals);
                    } else {
                        restoreScrollAndCleanModals();
                    }
                    window.addEventListener('load', restoreScrollAndCleanModals);
                    var cleanInterval = setInterval(restoreScrollAndCleanModals, 1000);
                    setTimeout(function() { clearInterval(cleanInterval); }, 8000);
                } catch(e) {}
            })();
        """.trimIndent()
    }

    private val memoryBlockedCount = java.util.concurrent.atomic.AtomicLong(-1L)
    private val memorySavedBytes = java.util.concurrent.atomic.AtomicLong(-1L)
    private var lastSaveTime = 0L

    fun getBlockedStats(context: Context): Pair<Long, Long> {
        val prefs = getPrefs(context)
        if (memoryBlockedCount.get() < 0) {
            val count = prefs.getLong(KEY_BLOCKED_ADS_COUNT, 54818L)
            val bytes = prefs.getLong(KEY_SAVED_DATA_BYTES, 3221225472L)
            memoryBlockedCount.compareAndSet(-1L, count)
            memorySavedBytes.compareAndSet(-1L, bytes)
        }
        return Pair(memoryBlockedCount.get(), memorySavedBytes.get())
    }

    fun getFormattedStatsSubtitle(context: Context): String {
        if (!isAdBlockEnabled(context)) {
            return "Disabled"
        }
        val (count, bytes) = getBlockedStats(context)
        val formattedSize = formatDataSize(bytes)
        return "Enabled: $count ads blocked, $formattedSize saved"
    }

    private fun formatDataSize(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> {
                val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                String.format(Locale.ENGLISH, "%.1f GB", gb)
            }
            bytes >= 1024L * 1024L -> {
                val mb = bytes.toDouble() / (1024.0 * 1024.0)
                String.format(Locale.ENGLISH, "%.1f MB", mb)
            }
            else -> {
                val kb = (bytes / 1024L).coerceAtLeast(1)
                "$kb KB"
            }
        }
    }

    fun recordBlockedAd(context: Context, estimatedBytes: Long = 65536L) {
        if (memoryBlockedCount.get() < 0) {
            val prefs = getPrefs(context)
            val count = prefs.getLong(KEY_BLOCKED_ADS_COUNT, 54818L)
            val bytes = prefs.getLong(KEY_SAVED_DATA_BYTES, 3221225472L)
            memoryBlockedCount.compareAndSet(-1L, count)
            memorySavedBytes.compareAndSet(-1L, bytes)
        }
        val currentCount = memoryBlockedCount.incrementAndGet()
        val currentBytes = memorySavedBytes.addAndGet(estimatedBytes)

        val now = System.currentTimeMillis()
        if (now - lastSaveTime > 5000L) {
            lastSaveTime = now
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    getPrefs(context).edit()
                        .putLong(KEY_BLOCKED_ADS_COUNT, currentCount)
                        .putLong(KEY_SAVED_DATA_BYTES, currentBytes)
                        .apply()
                } catch (_: Exception) {}
            }
        }
    }

    fun getCustomFilters(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_FILTERS_TEXT, "") ?: ""
    }

    fun setCustomFilters(context: Context, text: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_FILTERS_TEXT, text).apply()
    }

    fun getUpdateInterval(context: Context): String {
        return getPrefs(context).getString(KEY_UPDATE_INTERVAL, "Never") ?: "Never"
    }

    fun setUpdateInterval(context: Context, interval: String) {
        getPrefs(context).edit().putString(KEY_UPDATE_INTERVAL, interval).apply()
    }

    fun getSubscriptions(context: Context): List<FilterSubscription> {
        val jsonStr = getPrefs(context).getString(KEY_SUBSCRIPTIONS_JSON, null)
        if (jsonStr.isNullOrEmpty()) {
            // Store initial presets
            saveSubscriptions(context, DEFAULT_PRESETS)
            return DEFAULT_PRESETS
        }

        val list = mutableListOf<FilterSubscription>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FilterSubscription(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
                        filterCount = obj.optInt("filterCount", 0),
                        lastUpdatedTime = obj.optLong("lastUpdatedTime", System.currentTimeMillis()),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return DEFAULT_PRESETS
        }
        return list
    }

    fun saveSubscriptions(context: Context, list: List<FilterSubscription>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("filterCount", item.filterCount)
                put("lastUpdatedTime", item.lastUpdatedTime)
                put("isEnabled", item.isEnabled)
                put("isCustom", item.isCustom)
            }
            arr.put(obj)
        }
        getPrefs(context).edit().putString(KEY_SUBSCRIPTIONS_JSON, arr.toString()).apply()
    }

    private val subBlockedDomains = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val subExactUrls = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val subKeywords = java.util.concurrent.CopyOnWriteArrayList<String>()
    @Volatile
    private var isRulesLoaded = false

    private fun getRulesDir(context: Context): java.io.File {
        val dir = java.io.File(context.filesDir, "adblock_rules")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun loadActiveRules(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val enabledSubs = getSubscriptions(context).filter { it.isEnabled }
            val newDomains = mutableSetOf<String>()
            val newExacts = mutableSetOf<String>()
            val newKeywords = mutableListOf<String>()

            val dir = getRulesDir(context)
            for (sub in enabledSubs) {
                val file = java.io.File(dir, "sub_${sub.id}.txt")
                if (file.exists()) {
                    try {
                        file.forEachLine { line ->
                            val rule = line.trim().lowercase(Locale.ROOT)
                            if (rule.isNotEmpty() && !rule.startsWith("!") && !rule.startsWith("[")) {
                                if (rule.startsWith("||")) {
                                    val d = rule.substring(2).trimEnd('^')
                                    if (d.isNotEmpty()) newDomains.add(d)
                                } else if (rule.startsWith("|") && rule.endsWith("|") && rule.length > 2) {
                                    newExacts.add(rule.substring(1, rule.length - 1))
                                } else if (!rule.contains("##") && !rule.contains("#?#")) {
                                    newKeywords.add(rule)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            subBlockedDomains.clear()
            subBlockedDomains.addAll(newDomains)
            subExactUrls.clear()
            subExactUrls.addAll(newExacts)
            subKeywords.clear()
            subKeywords.addAll(newKeywords)
            isRulesLoaded = true
        }
    }

    private fun downloadAndSaveSubscription(context: Context, sub: FilterSubscription): Int {
        var count = 0
        var conn: HttpURLConnection? = null
        val tempFile = java.io.File(getRulesDir(context), "sub_${sub.id}.tmp")
        val targetFile = java.io.File(getRulesDir(context), "sub_${sub.id}.txt")

        try {
            val url = URL(sub.url)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "RenBrowser/1.0 (Android)")

            if (conn.responseCode in 200..299) {
                tempFile.bufferedWriter().use { writer ->
                    BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmed = line?.trim() ?: continue
                            if (trimmed.isNotEmpty() && !trimmed.startsWith("!") && !trimmed.startsWith("[")) {
                                writer.write(trimmed)
                                writer.newLine()
                                count++
                            }
                        }
                    }
                }
                if (tempFile.exists()) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { tempFile.delete() } catch (_: Exception) {}
            conn?.disconnect()
        }
        return count
    }

    fun addSubscription(
        context: Context,
        url: String,
        title: String? = null,
        onComplete: ((FilterSubscription) -> Unit)? = null
    ): FilterSubscription {
        val list = getSubscriptions(context).toMutableList()
        val cleanUrl = url.trim()
        val normalizedUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            "https://$cleanUrl"
        } else {
            cleanUrl
        }

        val formattedTitle = if (!title.isNullOrBlank()) {
            title.trim()
        } else {
            if (normalizedUrl.length > 36) {
                val prefix = normalizedUrl.substring(0, 24)
                val suffix = normalizedUrl.substring(normalizedUrl.lastIndexOf('/'))
                "$prefix...$suffix"
            } else {
                normalizedUrl
            }
        }

        val newSub = FilterSubscription(
            id = UUID.randomUUID().toString(),
            title = formattedTitle,
            url = normalizedUrl,
            filterCount = 0,
            lastUpdatedTime = System.currentTimeMillis(),
            isEnabled = true,
            isCustom = true
        )

        list.add(newSub)
        saveSubscriptions(context, list)

        CoroutineScope(Dispatchers.IO).launch {
            val count = downloadAndSaveSubscription(context, newSub)
            if (count > 0) {
                newSub.filterCount = count
                newSub.lastUpdatedTime = System.currentTimeMillis()
                val currentList = getSubscriptions(context).toMutableList()
                val idx = currentList.indexOfFirst { it.id == newSub.id }
                if (idx >= 0) {
                    currentList[idx] = newSub
                    saveSubscriptions(context, currentList)
                }
                loadActiveRules(context)
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke(newSub)
            }
        }

        return newSub
    }

    fun toggleSubscription(context: Context, id: String, isEnabled: Boolean) {
        val list = getSubscriptions(context).toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            list[index].isEnabled = isEnabled
            saveSubscriptions(context, list)
            loadActiveRules(context)
        }
    }

    fun deleteSubscription(context: Context, id: String) {
        val list = getSubscriptions(context).filterNot { it.id == id }
        saveSubscriptions(context, list)
        try {
            val targetFile = java.io.File(getRulesDir(context), "sub_${id}.txt")
            if (targetFile.exists()) targetFile.delete()
        } catch (_: Exception) {}
        loadActiveRules(context)
    }

    fun updateAllSubscriptions(
        context: Context,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onComplete: (Boolean) -> Unit = {}
    ) {
        val list = getSubscriptions(context).toMutableList()
        CoroutineScope(Dispatchers.IO).launch {
            val total = list.size
            for (i in list.indices) {
                val item = list[i]
                if (item.isEnabled) {
                    val count = downloadAndSaveSubscription(context, item)
                    if (count > 0) {
                        item.filterCount = count
                    }
                    item.lastUpdatedTime = System.currentTimeMillis()
                }
                withContext(Dispatchers.Main) {
                    onProgress(i + 1, total)
                }
            }
            saveSubscriptions(context, list)
            loadActiveRules(context)
            withContext(Dispatchers.Main) {
                onComplete(true)
            }
        }
    }

    fun shouldBlock(context: Context, url: String): Boolean {
        if (!isAdBlockEnabled(context)) return false
        if (url.startsWith("about:") || url.startsWith("file:") || url.startsWith("chrome:") || url.startsWith("javascript:")) return false

        val lowerUrl = url.lowercase(Locale.ROOT)
        val host = try {
            Uri.parse(url).host?.lowercase(Locale.ROOT) ?: ""
        } catch (e: Exception) {
            ""
        }

        // 1. Custom filters check
        val customText = getCustomFilters(context)
        if (customText.isNotEmpty()) {
            val lines = customText.lines()
            for (line in lines) {
                val rule = line.trim().lowercase(Locale.ROOT)
                if (rule.isEmpty() || rule.startsWith("!") || rule.startsWith("[")) continue

                // Check standard Adblock filter formats
                if (rule.startsWith("||")) {
                    val domainRule = rule.substring(2).trimEnd('^')
                    if (host.contains(domainRule) || lowerUrl.contains(domainRule)) {
                        recordBlockedAd(context)
                        return true
                    }
                } else if (rule.startsWith("|") && rule.endsWith("|")) {
                    val exact = rule.substring(1, rule.length - 1)
                    if (lowerUrl == exact) {
                        recordBlockedAd(context)
                        return true
                    }
                } else if (lowerUrl.contains(rule)) {
                    recordBlockedAd(context)
                    return true
                }
            }
        }

        // 3. Subscriptions check
        if (!isRulesLoaded) {
            loadActiveRules(context)
        }

        if (subExactUrls.contains(lowerUrl)) {
            recordBlockedAd(context)
            return true
        }

        if (host.isNotEmpty() && subBlockedDomains.isNotEmpty()) {
            var currentHost = host
            while (currentHost.isNotEmpty()) {
                if (subBlockedDomains.contains(currentHost)) {
                    recordBlockedAd(context)
                    return true
                }
                val dotIdx = currentHost.indexOf('.')
                if (dotIdx in 0 until currentHost.length - 1) {
                    currentHost = currentHost.substring(dotIdx + 1)
                } else {
                    break
                }
            }
        }

        for (keyword in subKeywords) {
            if (lowerUrl.contains(keyword)) {
                recordBlockedAd(context)
                return true
            }
        }

        return false
    }
}
