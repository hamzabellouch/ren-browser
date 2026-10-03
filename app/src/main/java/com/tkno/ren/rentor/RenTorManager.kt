package com.tkno.ren.rentor

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.GeolocationPermissions
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.tkno.ren.util.WarpManager
import java.util.concurrent.Executor

/**
 * High-level manager orchestrating Ren-Tor Engine preferences, WebView proxy overrides,
 * leak prevention, and system lifecycle integration.
 */
object RenTorManager {
    private const val TAG = "RenTorManager"
    private const val PREFS_NAME = "ren_tor_prefs"

    private const val KEY_TOR_ENABLED = "tor_enabled"
    private const val KEY_PROXY_HOST = "tor_proxy_host"
    private const val KEY_PROXY_PORT = "tor_proxy_port"
    private const val KEY_CONTROL_PORT = "tor_control_port"
    private const val KEY_DNS_PORT = "tor_dns_port"
    private const val KEY_BLOCK_GEOLOCATION = "tor_block_geolocation"
    private const val KEY_USE_BRIDGES = "tor_use_bridges"
    private const val KEY_BRIDGE_TYPE = "tor_bridge_type"
    private const val KEY_CUSTOM_BRIDGES = "tor_custom_bridges"
    private const val KEY_EXIT_COUNTRY = "tor_exit_country"
    private const val KEY_STRICT_NODES = "tor_strict_nodes"

    private const val DEFAULT_HOST = "127.0.0.1"
    private const val DEFAULT_PORT = 9050
    private const val DEFAULT_CONTROL_PORT = 9051
    private const val DEFAULT_DNS_PORT = 9053

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isTorEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TOR_ENABLED, false)
    }

    fun setTorEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TOR_ENABLED, enabled).apply()
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

    fun getControlPort(context: Context): Int {
        return getPrefs(context).getInt(KEY_CONTROL_PORT, DEFAULT_CONTROL_PORT)
    }

    fun setControlPort(context: Context, port: Int) {
        getPrefs(context).edit().putInt(KEY_CONTROL_PORT, port).apply()
    }

    fun getDnsPort(context: Context): Int {
        return getPrefs(context).getInt(KEY_DNS_PORT, DEFAULT_DNS_PORT)
    }

    fun setDnsPort(context: Context, port: Int) {
        getPrefs(context).edit().putInt(KEY_DNS_PORT, port).apply()
    }

    fun isBlockGeolocation(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_GEOLOCATION, true)
    }

    fun setBlockGeolocation(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_GEOLOCATION, enabled).apply()
    }

    fun isUseBridgesEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_USE_BRIDGES, false)
    }

    fun setUseBridgesEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USE_BRIDGES, enabled).apply()
    }

    fun getBridgeType(context: Context): BridgeType {
        val code = getPrefs(context).getString(KEY_BRIDGE_TYPE, BridgeType.NONE.code) ?: BridgeType.NONE.code
        return BridgeType.fromCode(code)
    }

    fun setBridgeType(context: Context, type: BridgeType) {
        getPrefs(context).edit().putString(KEY_BRIDGE_TYPE, type.code).apply()
    }

    fun getCustomBridges(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_BRIDGES, "") ?: ""
    }

    fun setCustomBridges(context: Context, bridges: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_BRIDGES, bridges.trim()).apply()
    }

    fun getExitCountry(context: Context): String? {
        val code = getPrefs(context).getString(KEY_EXIT_COUNTRY, null)
        return if (code.isNullOrBlank()) null else code
    }

    fun setExitCountry(context: Context, countryCode: String?) {
        getPrefs(context).edit().putString(KEY_EXIT_COUNTRY, countryCode?.trim()?.lowercase()).apply()
        if (isTorEnabled(context) && RenTorEngine.isConnected) {
            RenTorEngine.setExitCountry(countryCode, isStrictNodes(context))
        }
    }

    fun isStrictNodes(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STRICT_NODES, false)
    }

    fun setStrictNodes(context: Context, strict: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STRICT_NODES, strict).apply()
    }

    /**
     * Builds the complete settings snapshot for initializing RenTorEngine.
     */
    fun getSettings(context: Context): RenTorSettings {
        val custom = getCustomBridges(context)
            .split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return RenTorSettings(
            socksPort = getProxyPort(context),
            controlPort = getControlPort(context),
            dnsPort = getDnsPort(context),
            bridgeType = if (isUseBridgesEnabled(context)) getBridgeType(context) else BridgeType.NONE,
            customBridges = custom,
            preferredExitCountry = getExitCountry(context),
            strictNodes = isStrictNodes(context),
            blockGeolocation = isBlockGeolocation(context)
        )
    }

    /**
     * Applies Strict Zero-Leak Proxy configuration to Android System WebView.
     * All direct traffic is denied to prevent real IP leaks.
     */
    fun applyProxyOverride(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Log.w(TAG, "Proxy override is not supported on this Android WebView engine.")
            onComplete?.invoke(false)
            return
        }

        val host = getProxyHost(context)
        val port = getProxyPort(context)

        try {
            // Strict SOCKS5h routing - NO DIRECT fallback allowed
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("socks5://$host:$port")
                .addProxyRule("socks://$host:$port")
                .build()

            ProxyController.getInstance().setProxyOverride(
                proxyConfig,
                ContextCompat.getMainExecutor(context),
                {
                    if (isBlockGeolocation(context)) {
                        GeolocationPermissions.getInstance().clearAll()
                    }
                    Log.d(TAG, "Ren-Tor Zero-Leak Proxy strictly applied to WebView: socks5://$host:$port")
                    onComplete?.invoke(true)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error applying Ren-Tor Proxy override", e)
            onComplete?.invoke(false)
        }
    }

    /**
     * Clears proxy configuration from WebView.
     */
    fun clearProxyOverride(context: Context? = null, onComplete: ((Boolean) -> Unit)? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onComplete?.invoke(true)
            return
        }

        try {
            val executor: Executor = if (context != null) {
                ContextCompat.getMainExecutor(context)
            } else {
                Executor { runnable -> runnable.run() }
            }

            ProxyController.getInstance().clearProxyOverride(
                executor,
                {
                    Log.d(TAG, "Ren-Tor Proxy cleared from WebView.")
                    onComplete?.invoke(true)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing Ren-Tor Proxy override", e)
            onComplete?.invoke(false)
        }
    }

    /**
     * Toggles Ren-Tor on or off.
     */
    fun toggleTor(
        context: Context,
        onProgress: ((Int, String) -> Unit)? = null,
        onComplete: ((Boolean, Boolean) -> Unit)? = null
    ) {
        val newEnabled = !isTorEnabled(context)
        setTorEnabled(context, newEnabled)

        if (newEnabled) {
            // Ensure WARP is disabled to prevent proxy collisions
            if (WarpManager.isWarpEnabled(context)) {
                WarpManager.setWarpEnabled(context, false)
            }

            val settings = getSettings(context)
            RenTorEngine.start(
                context = context,
                settings = settings,
                onProgress = { progress, msg ->
                    onProgress?.invoke(progress, msg)
                },
                onComplete = { success, _ ->
                    if (success) {
                        applyProxyOverride(context) { proxySuccess ->
                            if (proxySuccess) {
                                onComplete?.invoke(true, true)
                            } else {
                                setTorEnabled(context, false)
                                onComplete?.invoke(false, false)
                            }
                        }
                    } else {
                        setTorEnabled(context, false)
                        clearProxyOverride(context) {
                            onComplete?.invoke(false, false)
                        }
                    }
                }
            )
        } else {
            setTorEnabled(context, false)
            RenTorEngine.stop(context)
            clearProxyOverride(context) { success ->
                onComplete?.invoke(false, success)
            }
        }
    }

    /**
     * Renews the current Ren-Tor circuit and identity (NEWNYM).
     */
    fun renewIdentity(onResult: (Boolean, String) -> Unit) {
        RenTorEngine.renewIdentity(onResult)
    }

    /**
     * Initializes Ren-Tor at application startup if enabled by user.
     */
    fun initAtStartup(context: Context) {
        if (isTorEnabled(context)) {
            val settings = getSettings(context)
            RenTorEngine.start(context, settings) { success, _ ->
                if (success) {
                    applyProxyOverride(context)
                } else {
                    setTorEnabled(context, false)
                    clearProxyOverride(context)
                }
            }
        }
    }
}
