package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.GeolocationPermissions
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

enum class TorStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

object TorManager {
    private const val TAG = "TorManager"
    private const val PREFS_NAME = "ren_tor_prefs"
    private const val KEY_TOR_ENABLED = "tor_enabled"
    private const val KEY_PROXY_HOST = "tor_proxy_host"
    private const val KEY_PROXY_PORT = "tor_proxy_port"
    private const val KEY_BLOCK_GEOLOCATION = "tor_block_geolocation"

    private const val DEFAULT_HOST = "127.0.0.1"
    private const val DEFAULT_PORT = 9050

    var status: TorStatus = TorStatus.DISCONNECTED
        private set

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isTorEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TOR_ENABLED, false)
    }

    fun setTorEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TOR_ENABLED, enabled).apply()
        if (!enabled) {
            status = TorStatus.DISCONNECTED
        }
    }

    fun getProxyHost(context: Context): String {
        return getPrefs(context).getString(KEY_PROXY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
    }

    fun setProxyHost(context: Context, host: String) {
        getPrefs(context).edit().putString(KEY_PROXY_HOST, host.trim()).apply()
    }

    fun getProxyPort(context: Context): Int {
        return getPrefs(context).getInt(KEY_PROXY_PORT, DEFAULT_PORT)
    }

    fun setProxyPort(context: Context, port: Int) {
        getPrefs(context).edit().putInt(KEY_PROXY_PORT, port).apply()
    }

    fun isBlockGeolocation(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_GEOLOCATION, true)
    }

    fun setBlockGeolocation(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_GEOLOCATION, enabled).apply()
    }

    fun applyProxyOverride(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Log.w(TAG, "Proxy override is not supported on this Android WebView version.")
            status = TorStatus.ERROR
            onComplete?.invoke(false)
            return
        }

        val host = getProxyHost(context)
        val port = getProxyPort(context)
        status = TorStatus.CONNECTING

        try {
            // Strict Tor Routing: NO .addDirect() to prevent any real IP leaks!
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("socks5://$host:$port")
                .addProxyRule("socks://$host:$port")
                .build()

            ProxyController.getInstance().setProxyOverride(
                proxyConfig,
                { runnable -> runnable.run() },
                {
                    status = TorStatus.CONNECTED
                    if (isBlockGeolocation(context)) {
                        GeolocationPermissions.getInstance().clearAll()
                    }
                    Log.d(TAG, "Tor Proxy strictly applied to WebView: socks5://$host:$port (No Direct Fallback)")
                    onComplete?.invoke(true)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error applying Tor Proxy override", e)
            status = TorStatus.ERROR
            onComplete?.invoke(false)
        }
    }

    fun clearProxyOverride(onComplete: ((Boolean) -> Unit)? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            status = TorStatus.DISCONNECTED
            onComplete?.invoke(true)
            return
        }

        try {
            ProxyController.getInstance().clearProxyOverride(
                { runnable -> runnable.run() },
                {
                    status = TorStatus.DISCONNECTED
                    Log.d(TAG, "Tor Proxy cleared from WebView.")
                    onComplete?.invoke(true)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing Tor Proxy override", e)
            status = TorStatus.DISCONNECTED
            onComplete?.invoke(false)
        }
    }

    fun toggleTor(
        context: Context,
        onProgress: ((Int, String) -> Unit)? = null,
        onComplete: ((Boolean, Boolean) -> Unit)? = null
    ) {
        val newEnabled = !isTorEnabled(context)
        setTorEnabled(context, newEnabled)

        if (newEnabled) {
            status = TorStatus.CONNECTING
            TorEngine.startTor(
                context = context,
                onProgress = { progress, msg ->
                    onProgress?.invoke(progress, msg)
                },
                onComplete = { success, _ ->
                    if (success) {
                        applyProxyOverride(context) { proxySuccess ->
                            onComplete?.invoke(true, proxySuccess)
                        }
                    } else {
                        // Even if daemon startup is in fallback, apply proxy override so connection is locked to Tor port
                        applyProxyOverride(context) { proxySuccess ->
                            onComplete?.invoke(true, proxySuccess)
                        }
                    }
                }
            )
        } else {
            TorEngine.stopTor(context)
            clearProxyOverride { success ->
                onComplete?.invoke(false, success)
            }
        }
    }

    fun initAtStartup(context: Context) {
        if (isTorEnabled(context)) {
            TorEngine.startTor(context) { success, _ ->
                if (success) {
                    applyProxyOverride(context)
                }
            }
        }
    }
}
