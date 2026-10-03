package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executor

enum class WarpStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class WarpSecurityMode(val id: String, val title: String, val dohEndpoint: String) {
    STANDARD("standard", "Standard (1.1.1.1)", "https://cloudflare-dns.com/dns-query"),
    MALWARE("malware", "Block Malware (1.1.1.2)", "https://security.cloudflare-dns.com/dns-query"),
    FAMILIES("families", "Malware + Adult Content (1.1.1.3)", "https://family.cloudflare-dns.com/dns-query")
}

object WarpManager {
    private const val TAG = "WarpManager"
    private const val PREFS_NAME = "ren_warp_prefs"
    private const val KEY_WARP_ENABLED = "warp_enabled"
    private const val KEY_PROXY_HOST = "warp_proxy_host"
    private const val KEY_PROXY_PORT = "warp_proxy_port"
    private const val KEY_SECURITY_MODE = "warp_security_mode"
    private const val KEY_LICENSE_KEY = "warp_license_key"
    private const val KEY_DOH_ENABLED = "warp_doh_enabled"

    const val DEFAULT_HOST = "127.0.0.1"
    const val DEFAULT_PORT = 9060

    var status: WarpStatus = WarpStatus.DISCONNECTED
        private set

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isWarpEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WARP_ENABLED, false)
    }

    fun setWarpEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WARP_ENABLED, enabled).apply()
        if (!enabled) {
            status = WarpStatus.DISCONNECTED
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

    fun getSecurityMode(context: Context): WarpSecurityMode {
        val id = getPrefs(context).getString(KEY_SECURITY_MODE, WarpSecurityMode.STANDARD.id)
        return WarpSecurityMode.values().find { it.id == id } ?: WarpSecurityMode.STANDARD
    }

    fun setSecurityMode(context: Context, mode: WarpSecurityMode) {
        getPrefs(context).edit().putString(KEY_SECURITY_MODE, mode.id).apply()
    }

    fun getLicenseKey(context: Context): String {
        return getPrefs(context).getString(KEY_LICENSE_KEY, "") ?: ""
    }

    fun setLicenseKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_LICENSE_KEY, key.trim()).apply()
    }

    fun isDohEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DOH_ENABLED, true)
    }

    fun setDohEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DOH_ENABLED, enabled).apply()
    }

    /**
     * Checks whether the configured proxy server is listening and accepting socket connections.
     */
    fun isProxyPortOpen(host: String = DEFAULT_HOST, port: Int = DEFAULT_PORT, timeoutMs: Int = 1200): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun checkProxyHealth(
        context: Context,
        onResult: (isOpen: Boolean, message: String) -> Unit
    ) {
        val host = getProxyHost(context)
        val port = getProxyPort(context)
        if (host == DEFAULT_HOST && CloudflareEngine.isRunning) {
            onResult(true, "Cloudflare Engine active and running on $host:$port")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val open = isProxyPortOpen(host, port, 1500)
            withContext(Dispatchers.Main) {
                val msg = if (open) {
                    "Proxy active and reachable on $host:$port"
                } else {
                    "No active proxy service detected on $host:$port"
                }
                onResult(open, msg)
            }
        }
    }

    fun applyProxyOverride(context: Context, onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Log.w(TAG, "Proxy override is not supported on this Android WebView version.")
            status = WarpStatus.ERROR
            onComplete?.invoke(false, "WebView Proxy Override not supported on this device")
            return
        }

        val host = getProxyHost(context)
        val port = getProxyPort(context)
        status = WarpStatus.CONNECTING

        try {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("http://$host:$port")
                .addProxyRule("socks5://$host:$port")
                .build()

            ProxyController.getInstance().setProxyOverride(
                proxyConfig,
                ContextCompat.getMainExecutor(context),
                {
                    status = WarpStatus.CONNECTED
                    Log.d(TAG, "Cloudflare Proxy applied to WebView: $host:$port")
                    onComplete?.invoke(true, null)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error applying Cloudflare Proxy override", e)
            status = WarpStatus.ERROR
            onComplete?.invoke(false, e.localizedMessage)
        }
    }

    fun clearProxyOverride(context: Context? = null, onComplete: ((Boolean) -> Unit)? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            status = WarpStatus.DISCONNECTED
            onComplete?.invoke(true)
            return
        }

        try {
            val executor: Executor = if (context != null) ContextCompat.getMainExecutor(context) else Executor { runnable -> runnable.run() }
            ProxyController.getInstance().clearProxyOverride(
                executor,
                {
                    status = WarpStatus.DISCONNECTED
                    Log.d(TAG, "Cloudflare Proxy cleared from WebView.")
                    onComplete?.invoke(true)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing Cloudflare Proxy override", e)
            status = WarpStatus.DISCONNECTED
            onComplete?.invoke(false)
        }
    }

    fun toggleWarp(
        context: Context,
        onProgress: ((Int, String) -> Unit)? = null,
        onComplete: ((Boolean, Boolean, String?) -> Unit)? = null
    ) {
        val willEnable = !isWarpEnabled(context)

        if (willEnable) {
            status = WarpStatus.CONNECTING
            onProgress?.invoke(25, "Starting Cloudflare 1.1.1.1 Engine...")

            val host = getProxyHost(context)
            val port = getProxyPort(context)

            // If Tor is currently enabled, disable Tor to avoid proxy conflicts
            if (TorManager.isTorEnabled(context)) {
                TorManager.setTorEnabled(context, false)
                TorEngine.stopTor(context)
                TorManager.clearProxyOverride(context)
            }

            if (host == DEFAULT_HOST || host == "localhost") {
                // Launch native embedded Cloudflare engine
                CloudflareEngine.start(context, port) { success, errorMsg ->
                    if (success) {
                        setWarpEnabled(context, true)
                        onProgress?.invoke(70, "Connecting WebView to Cloudflare...")
                        applyProxyOverride(context) { proxySuccess, err ->
                            if (proxySuccess) {
                                status = WarpStatus.CONNECTED
                                onProgress?.invoke(100, "Cloudflare connected successfully")
                                onComplete?.invoke(true, true, null)
                            } else {
                                status = WarpStatus.ERROR
                                setWarpEnabled(context, false)
                                CloudflareEngine.stop()
                                clearProxyOverride(context)
                                onComplete?.invoke(false, false, err ?: "Failed to set WebView proxy")
                            }
                        }
                    } else {
                        status = WarpStatus.ERROR
                        setWarpEnabled(context, false)
                        clearProxyOverride(context)
                        onComplete?.invoke(false, false, errorMsg ?: "Failed to start Cloudflare engine")
                    }
                }
            } else {
                // Custom remote proxy host
                CoroutineScope(Dispatchers.IO).launch {
                    val isPortOpen = isProxyPortOpen(host, port, 1500)
                    withContext(Dispatchers.Main) {
                        if (!isPortOpen) {
                            status = WarpStatus.ERROR
                            setWarpEnabled(context, false)
                            val errorMsg = "External WARP Proxy unreachable on $host:$port"
                            Log.w(TAG, errorMsg)
                            onComplete?.invoke(false, false, errorMsg)
                            return@withContext
                        }

                        setWarpEnabled(context, true)
                        applyProxyOverride(context) { success, err ->
                            if (success) {
                                status = WarpStatus.CONNECTED
                                onComplete?.invoke(true, true, null)
                            } else {
                                status = WarpStatus.ERROR
                                setWarpEnabled(context, false)
                                clearProxyOverride(context)
                                onComplete?.invoke(false, false, err ?: "Failed to apply proxy override")
                            }
                        }
                    }
                }
            }
        } else {
            setWarpEnabled(context, false)
            CloudflareEngine.stop()
            clearProxyOverride(context) { success ->
                onComplete?.invoke(false, success, null)
            }
        }
    }

    fun initAtStartup(context: Context) {
        if (isWarpEnabled(context) && !TorManager.isTorEnabled(context)) {
            val host = getProxyHost(context)
            val port = getProxyPort(context)
            if (host == DEFAULT_HOST || host == "localhost") {
                CloudflareEngine.start(context, port) { success, _ ->
                    if (success) {
                        applyProxyOverride(context)
                    } else {
                        setWarpEnabled(context, false)
                        clearProxyOverride(context)
                    }
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    if (isProxyPortOpen(host, port, 1200)) {
                        withContext(Dispatchers.Main) {
                            applyProxyOverride(context)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            setWarpEnabled(context, false)
                            clearProxyOverride(context)
                        }
                    }
                }
            }
        }
    }

    fun verifyWarpConnection(
        context: Context,
        onResult: (isWarp: Boolean, ip: String?, loc: String?, colo: String?, message: String) -> Unit
    ) {
        val host = getProxyHost(context)
        val port = getProxyPort(context)
        val isEnabled = isWarpEnabled(context)

        CoroutineScope(Dispatchers.IO).launch {
            val isEngineActive = CloudflareEngine.isRunning || isProxyPortOpen(host, port, 1500)
            var conn: HttpURLConnection? = null
            try {
                val url = URL("https://www.cloudflare.com/cdn-cgi/trace")

                conn = if (isEnabled && isEngineActive) {
                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", port))
                    url.openConnection(proxy) as HttpURLConnection
                } else {
                    url.openConnection() as HttpURLConnection
                }

                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "RenBrowser/1.0")

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val map = mutableMapOf<String, String>()
                    body.lines().forEach { line ->
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            map[parts[0].trim()] = parts[1].trim()
                        }
                    }

                    val warpVal = map["warp"] ?: "off"
                    val isWarp = warpVal == "on" || warpVal == "plus"
                    val ip = map["ip"] ?: "Unknown"
                    val loc = map["loc"] ?: "Unknown"
                    val colo = map["colo"] ?: "Unknown"

                    withContext(Dispatchers.Main) {
                        val statusMsg = when {
                            isWarp -> "✓ WARP Active ($warpVal) via Cloudflare Edge ($colo - $loc)"
                            isEnabled && isEngineActive -> "✓ Cloudflare Edge ($colo - $loc) • 1.1.1.1 DoH Active (IP: $ip)"
                            else -> "Direct Connection (Cloudflare Inactive). Real IP: $ip ($colo, $loc)"
                        }
                        onResult(isWarp || (isEnabled && isEngineActive), ip, loc, colo, statusMsg)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, null, null, null, "HTTP ${conn.responseCode} while checking Cloudflare trace")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = if (isEnabled && !isEngineActive) {
                        "Cloudflare offline: Cannot reach proxy at $host:$port"
                    } else {
                        "Connection check: ${e.localizedMessage}"
                    }
                    onResult(false, null, null, null, msg)
                }
            } finally {
                conn?.disconnect()
            }
        }
    }
}
