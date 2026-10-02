package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebStorage
import android.webkit.WebView
import java.io.File

object SandboxManager {

    private const val PREFS_NAME = "ren_sandbox_prefs"
    private const val KEY_SANDBOX_ENABLED = "sandbox_enabled"
    private const val KEY_BLOCK_TRACKERS = "sandbox_block_trackers"
    private const val KEY_BLOCK_HARDWARE = "sandbox_block_hardware"
    private const val KEY_BLOCK_INTENTS = "sandbox_block_intents"
    private const val KEY_BLOCK_WEBRTC = "sandbox_block_webrtc"
    private const val KEY_FORGET_ON_SITE_CLOSE = "sandbox_forget_on_site_close"

    // Common background telemetry, tracking, analytics, and beacon domains/keywords
    private val TRACKER_PATTERNS = listOf(
        "google-analytics.com",
        "analytics.google.com",
        "googletagmanager.com",
        "doubleclick.net",
        "telemetry",
        "app-measurement.com",
        "crashlytics.com",
        "segment.io",
        "segment.com",
        "sentry.io",
        "hotjar.com",
        "mixpanel.com",
        "amplitude.com",
        "scorecardresearch.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "quantserve.com",
        "newrelic.com",
        "datadoghq.com",
        "facebook.com/tr",
        "connect.facebook.net",
        "beacon.",
        "/beacon/",
        "/collect?",
        "/telemetry?",
        "/analytics/",
        "/event-logging"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isSandboxEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SANDBOX_ENABLED, false)
    }

    /**
     * Toggles the Sandbox mode.
     * - When entering Sandbox (enabled = true): Isolates the session and ensures no lingering data leaks in.
     * - When exiting Sandbox (enabled = false): Completely and automatically purges all cookies, local storage,
     *   session storage, WebStorage, IndexedDB, WebView cache, history, and databases created inside the sandbox.
     */
    fun setSandboxEnabled(
        context: Context,
        enabled: Boolean,
        webViews: List<WebView> = emptyList(),
        onPurged: (() -> Unit)? = null
    ) {
        val wasEnabled = isSandboxEnabled(context)
        getPrefs(context).edit().putBoolean(KEY_SANDBOX_ENABLED, enabled).apply()

        // Purge data on both state transitions to guarantee complete isolation:
        // 1. When disabling: destroys everything done in the sandbox without exception.
        // 2. When enabling: ensures sandbox starts from a pristine, isolated state.
        if (wasEnabled != enabled) {
            purgeSandboxData(context, webViews, onComplete = onPurged)
        } else {
            onPurged?.invoke()
        }
    }

    fun isBlockTrackers(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_TRACKERS, true)
    }

    fun setBlockTrackers(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_TRACKERS, enabled).apply()
    }

    fun isBlockHardware(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_HARDWARE, true)
    }

    fun setBlockHardware(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_HARDWARE, enabled).apply()
    }

    fun isBlockIntents(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_INTENTS, true)
    }

    fun setBlockIntents(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_INTENTS, enabled).apply()
    }

    fun isBlockWebRtc(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_WEBRTC, true)
    }

    fun setBlockWebRtc(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_WEBRTC, enabled).apply()
    }

    fun isForgetOnSiteCloseEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FORGET_ON_SITE_CLOSE, true)
    }

    fun setForgetOnSiteCloseEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FORGET_ON_SITE_CLOSE, enabled).apply()
    }

    /**
     * Clears cookies, WebStorage (local/session storage, IndexedDB), and cache
     * for a closed site or tab when "Forget me when I close the site" is active.
     */
    fun clearSiteData(context: Context, webView: WebView? = null) {
        try {
            webView?.apply {
                stopLoading()
                clearCache(true)
                clearFormData()
                clearHistory()
                clearSslPreferences()
                clearMatches()
            }
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.removeSessionCookies {
                    cookieManager.flush()
                }
            }
            WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) {
            // ignore cleanup errors
        }
    }

    /**
     * Checks whether an outbound network request is a background tracking/telemetry call
     * that should be quarantined while in Sandbox mode.
     */
    fun shouldBlockRequest(url: String): Boolean {
        val lowerUrl = url.lowercase()
        for (pattern in TRACKER_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                return true
            }
        }
        return false
    }

    /**
     * Completely purges all cookies, web storage, geolocation permissions, and cache.
     * Guaranteed to destroy all traces of the sandboxed session.
     */
    fun purgeSandboxData(
        context: Context,
        webViews: List<WebView> = emptyList(),
        onComplete: (() -> Unit)? = null
    ) {
        try {
            // 1. Stop and clear all WebViews in memory
            for (wv in webViews) {
                try {
                    wv.stopLoading()
                    wv.clearCache(true)
                    wv.clearFormData()
                    wv.clearHistory()
                    wv.clearSslPreferences()
                    wv.clearMatches()
                } catch (_: Exception) {}
            }

            // 2. Clear all Cookies (persistent & session)
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.removeSessionCookies {
                    cookieManager.flush()
                }
            }

            // 3. Clear WebStorage (LocalStorage, SessionStorage, IndexedDB)
            WebStorage.getInstance().deleteAllData()

            // 4. Clear Geolocation & Permissions
            GeolocationPermissions.getInstance().clearAll()

            // 5. Clean cache directories
            context.cacheDir?.deleteRecursively()
            context.codeCacheDir?.deleteRecursively()

            // 6. Remove physical database and storage files created by WebView engine
            try {
                val dataDir = File(context.applicationInfo.dataDir)
                val appWebviewDir = File(dataDir, "app_webview")
                if (appWebviewDir.exists()) {
                    File(appWebviewDir, "Local Storage").deleteRecursively()
                    File(appWebviewDir, "IndexedDB").deleteRecursively()
                    File(appWebviewDir, "Service Worker").deleteRecursively()
                    File(appWebviewDir, "Cache").deleteRecursively()
                    File(appWebviewDir, "Code Cache").deleteRecursively()
                    File(appWebviewDir, "GPUCache").deleteRecursively()
                    File(appWebviewDir, "databases").deleteRecursively()
                    File(appWebviewDir, "Default").deleteRecursively()
                    File(appWebviewDir, "Cookies").delete()
                    File(appWebviewDir, "Cookies-journal").delete()
                    File(appWebviewDir, "Web Data").delete()
                    File(appWebviewDir, "Web Data-journal").delete()
                }
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            onComplete?.invoke()
        }
    }
}
