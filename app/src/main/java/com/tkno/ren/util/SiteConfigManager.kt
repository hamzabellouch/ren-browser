package com.tkno.ren.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object SiteConfigManager {

    private const val PREFS_NAME = "ren_site_config_prefs"

    // --- Keys ---
    // Secure DNS (DoH)
    private const val KEY_SECURE_DNS_ENABLED = "secure_dns_enabled"
    private const val KEY_SECURE_DNS_MODE = "secure_dns_mode" // "system", "provider", "custom"
    private const val KEY_SECURE_DNS_PROVIDER = "secure_dns_provider" // "Google", "Cloudflare", "OpenDNS", "Quad9", "AdGuard"
    private const val KEY_SECURE_DNS_CUSTOM_URL = "secure_dns_custom_url"

    // Do Not Track & GPC
    private const val KEY_DO_NOT_TRACK = "do_not_track_enabled"

    // Cookies
    private const val KEY_COOKIES_MODE = "cookies_mode" // "allow_all", "block_third_party", "block_all"

    // JavaScript & V8 Engine
    private const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"
    private const val KEY_V8_OPTIMIZER_ENABLED = "v8_optimizer_enabled"

    // Permissions (Values: "ask", "allow", "block")
    private const val KEY_PERMISSION_LOCATION = "permission_location"
    private const val KEY_PERMISSION_CAMERA = "permission_camera"
    private const val KEY_PERMISSION_MICROPHONE = "permission_microphone"
    private const val KEY_PERMISSION_MOTION = "permission_motion_sensors"
    private const val KEY_PERMISSION_NFC = "permission_nfc"
    private const val KEY_PERMISSION_USB = "permission_usb"
    private const val KEY_PERMISSION_SERIAL = "permission_serial"
    private const val KEY_PERMISSION_FILE_EDITING = "permission_file_editing"
    private const val KEY_PERMISSION_KEYBOARD_LOCK = "permission_keyboard_lock"
    private const val KEY_PERMISSION_VR = "permission_vr"
    private const val KEY_PERMISSION_AR = "permission_ar"
    private const val KEY_PERMISSION_LOCAL_NETWORK = "permission_local_network"

    // Content Controls
    private const val KEY_POPUPS_BLOCKED = "popups_blocked"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_INTRUSIVE_ADS_BLOCKED = "intrusive_ads_blocked"
    private const val KEY_PROTECTED_CONTENT_ENABLED = "protected_content_enabled"
    private const val KEY_AUTOMATIC_DOWNLOADS = "automatic_downloads_permission" // "ask", "allow", "block"
    private const val KEY_INSECURE_CONTENT_BLOCKED = "insecure_content_blocked"
    private const val KEY_HTTPS_ONLY_MODE = "https_only_mode"

    // DNS Providers
    data class DnsProvider(
        val name: String,
        val description: String,
        val dohUrl: String,
        val ipAddress: String
    )

    val POPULAR_DNS_PROVIDERS = listOf(
        DnsProvider(
            name = "Google (Public DNS)",
            description = "High-speed, globally distributed Anycast DNS",
            dohUrl = "https://dns.google/dns-query",
            ipAddress = "8.8.8.8"
        ),
        DnsProvider(
            name = "Cloudflare (1.1.1.1)",
            description = "Fastest privacy-first DNS resolver with no logs",
            dohUrl = "https://cloudflare-dns.com/dns-query",
            ipAddress = "1.1.1.1"
        ),
        DnsProvider(
            name = "OpenDNS (Cisco)",
            description = "Reliable DNS with phishing protection and content filtering",
            dohUrl = "https://doh.opendns.com/dns-query",
            ipAddress = "208.67.222.222"
        ),
        DnsProvider(
            name = "Quad9 (Privacy & Security)",
            description = "Swiss-based privacy DNS blocking malicious domains",
            dohUrl = "https://dns.quad9.net/dns-query",
            ipAddress = "9.9.9.9"
        ),
        DnsProvider(
            name = "AdGuard DNS",
            description = "Blocks ads, trackers, and malicious sites at DNS level",
            dohUrl = "https://dns.adguard-dns.com/dns-query",
            ipAddress = "94.140.14.14"
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Secure DNS Getters & Setters ---

    fun isSecureDnsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SECURE_DNS_ENABLED, true)
    }

    fun setSecureDnsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SECURE_DNS_ENABLED, enabled).apply()
    }

    fun getSecureDnsMode(context: Context): String {
        return getPrefs(context).getString(KEY_SECURE_DNS_MODE, "provider") ?: "provider"
    }

    fun setSecureDnsMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_SECURE_DNS_MODE, mode).apply()
    }

    fun getSecureDnsProviderName(context: Context): String {
        return getPrefs(context).getString(KEY_SECURE_DNS_PROVIDER, "Cloudflare (1.1.1.1)") ?: "Cloudflare (1.1.1.1)"
    }

    fun setSecureDnsProviderName(context: Context, providerName: String) {
        getPrefs(context).edit().putString(KEY_SECURE_DNS_PROVIDER, providerName).apply()
    }

    fun getCustomDnsUrl(context: Context): String {
        return getPrefs(context).getString(KEY_SECURE_DNS_CUSTOM_URL, "https://") ?: "https://"
    }

    fun setCustomDnsUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_SECURE_DNS_CUSTOM_URL, url).apply()
    }

    fun getEffectiveDnsSummary(context: Context): String {
        if (!isSecureDnsEnabled(context)) {
            return "Disabled (Standard DNS)"
        }
        return when (getSecureDnsMode(context)) {
            "system" -> "System Service Provider"
            "custom" -> {
                val custom = getCustomDnsUrl(context)
                if (custom.isNotBlank() && custom != "https://") custom else "Custom DoH"
            }
            else -> getSecureDnsProviderName(context)
        }
    }

    fun getEffectiveDohUrl(context: Context): String? {
        if (!isSecureDnsEnabled(context)) return null
        return when (getSecureDnsMode(context)) {
            "system" -> null // Delegates directly to System / Android Private DNS
            "custom" -> {
                val custom = getCustomDnsUrl(context)
                if (custom.isNotBlank() && custom.startsWith("https://", ignoreCase = true)) custom else "https://cloudflare-dns.com/dns-query"
            }
            else -> {
                val providerName = getSecureDnsProviderName(context)
                POPULAR_DNS_PROVIDERS.find { it.name == providerName }?.dohUrl ?: "https://cloudflare-dns.com/dns-query"
            }
        }
    }

    private val dohCache = ConcurrentHashMap<String, Pair<List<String>, Long>>()

    fun resolveHostOverDoh(context: Context, host: String): List<String> {
        val dohUrl = getEffectiveDohUrl(context) ?: return emptyList()
        val cleanHost = host.trim().lowercase()
        if (cleanHost.isBlank() || isLocalNetworkTarget("http://$cleanHost")) return emptyList()

        val now = System.currentTimeMillis()
        val cached = dohCache[cleanHost]
        if (cached != null && cached.second > now) {
            return cached.first
        }

        try {
            val separator = if (dohUrl.contains("?")) "&" else "?"
            val queryUrl = "${dohUrl}${separator}name=${Uri.encode(cleanHost)}&type=A"

            val url = URL(queryUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/dns-json")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.connect()

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val answers = json.optJSONArray("Answer")
                val ips = mutableListOf<String>()
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val item = answers.getJSONObject(i)
                        val type = item.optInt("type", 1) // 1 = A (IPv4)
                        val data = item.optString("data", "")
                        if (type == 1 && data.isNotBlank()) {
                            ips.add(data)
                        }
                    }
                }
                if (ips.isNotEmpty()) {
                    dohCache[cleanHost] = Pair(ips, now + 300_000L) // 5 min TTL
                    return ips
                }
            }
        } catch (_: Exception) {}
        return emptyList()
    }

    // --- Do Not Track ---

    fun isDoNotTrackEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DO_NOT_TRACK, true)
    }

    fun setDoNotTrackEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DO_NOT_TRACK, enabled).apply()
    }

    // --- Cookies ---

    fun getCookiesMode(context: Context): String {
        return getPrefs(context).getString(KEY_COOKIES_MODE, "block_third_party") ?: "block_third_party"
    }

    fun setCookiesMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_COOKIES_MODE, mode).apply()
        applyCookiesPolicy(context)
    }

    fun applyCookiesPolicy(context: Context, webView: WebView? = null) {
        val cookieManager = CookieManager.getInstance()
        val mode = getCookiesMode(context)
        when (mode) {
            "allow_all" -> {
                cookieManager.setAcceptCookie(true)
                if (webView != null) {
                    cookieManager.setAcceptThirdPartyCookies(webView, true)
                }
            }
            "block_third_party" -> {
                cookieManager.setAcceptCookie(true)
                if (webView != null) {
                    cookieManager.setAcceptThirdPartyCookies(webView, false)
                }
            }
            "block_all" -> {
                cookieManager.setAcceptCookie(false)
                if (webView != null) {
                    cookieManager.setAcceptThirdPartyCookies(webView, false)
                }
            }
        }
    }

    // --- JavaScript & V8 Engine ---

    fun isJavaScriptEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_JAVASCRIPT_ENABLED, true)
    }

    fun setJavaScriptEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_JAVASCRIPT_ENABLED, enabled).apply()
    }

    fun isV8OptimizerEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_V8_OPTIMIZER_ENABLED, true)
    }

    fun setV8OptimizerEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_V8_OPTIMIZER_ENABLED, enabled).apply()
    }

    // --- Permissions Manager ---

    fun getLocationPermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_LOCATION, "ask") ?: "ask"
    }

    fun setLocationPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_LOCATION, value).apply()
    }

    fun getCameraPermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_CAMERA, "ask") ?: "ask"
    }

    fun setCameraPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_CAMERA, value).apply()
    }

    fun getMicrophonePermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_MICROPHONE, "ask") ?: "ask"
    }

    fun setMicrophonePermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_MICROPHONE, value).apply()
    }

    fun isMotionSensorsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PERMISSION_MOTION, true)
    }

    fun setMotionSensorsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PERMISSION_MOTION, enabled).apply()
    }

    fun getNfcPermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_NFC, "ask") ?: "ask"
    }

    fun setNfcPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_NFC, value).apply()
    }

    fun getUsbPermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_USB, "ask") ?: "ask"
    }

    fun setUsbPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_USB, value).apply()
    }

    fun getSerialPermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_SERIAL, "ask") ?: "ask"
    }

    fun setSerialPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_SERIAL, value).apply()
    }

    fun getFileEditingPermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_FILE_EDITING, "ask") ?: "ask"
    }

    fun setFileEditingPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_FILE_EDITING, value).apply()
    }

    fun isKeyboardLockAllowed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PERMISSION_KEYBOARD_LOCK, false)
    }

    fun setKeyboardLockAllowed(context: Context, allowed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PERMISSION_KEYBOARD_LOCK, allowed).apply()
    }

    fun getVrPermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_VR, "ask") ?: "ask"
    }

    fun setVrPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_VR, value).apply()
    }

    fun getArPermission(context: Context): String {
        return getPrefs(context).getString(KEY_PERMISSION_AR, "ask") ?: "ask"
    }

    fun setArPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PERMISSION_AR, value).apply()
    }

    fun isLocalNetworkAccessAllowed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PERMISSION_LOCAL_NETWORK, false)
    }

    fun setLocalNetworkAccessAllowed(context: Context, allowed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PERMISSION_LOCAL_NETWORK, allowed).apply()
    }

    // --- Content Controls ---

    fun isPopupsBlocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_POPUPS_BLOCKED, true)
    }

    fun setPopupsBlocked(context: Context, blocked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_POPUPS_BLOCKED, blocked).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isIntrusiveAdsBlocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INTRUSIVE_ADS_BLOCKED, true)
    }

    fun setIntrusiveAdsBlocked(context: Context, blocked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INTRUSIVE_ADS_BLOCKED, blocked).apply()
    }

    fun isProtectedContentEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PROTECTED_CONTENT_ENABLED, true)
    }

    fun setProtectedContentEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PROTECTED_CONTENT_ENABLED, enabled).apply()
    }

    fun getAutomaticDownloadsPermission(context: Context): String {
        return getPrefs(context).getString(KEY_AUTOMATIC_DOWNLOADS, "ask") ?: "ask"
    }

    fun setAutomaticDownloadsPermission(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_AUTOMATIC_DOWNLOADS, value).apply()
    }

    fun isInsecureContentBlocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INSECURE_CONTENT_BLOCKED, true)
    }

    fun setInsecureContentBlocked(context: Context, blocked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INSECURE_CONTENT_BLOCKED, blocked).apply()
    }

    fun isHttpsOnlyMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HTTPS_ONLY_MODE, true)
    }

    fun setHttpsOnlyMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HTTPS_ONLY_MODE, enabled).apply()
    }

    // --- Runtime Enforcement & WebView Application ---

    @SuppressLint("SetJavaScriptEnabled")
    fun applyToWebView(context: Context, webView: WebView) {
        val settings = webView.settings
        val isJs = isJavaScriptEnabled(context)

        settings.javaScriptEnabled = isJs
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)

        // Geolocation
        val locPerm = getLocationPermission(context)
        settings.setGeolocationEnabled(locPerm != "block")

        // Media & Sound
        val soundOn = isSoundEnabled(context)
        settings.mediaPlaybackRequiresUserGesture = !soundOn

        // Mixed / Insecure Content
        val blockInsecure = isInsecureContentBlocked(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = if (blockInsecure) {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }

        // Apply Cookies Policy
        applyCookiesPolicy(context, webView)
    }

    /**
     * Checks whether an URL targets the local private network or localhost.
     * Used when local network access is blocked for security against internal port scanning / intranet pivoting.
     */
    fun isLocalNetworkTarget(url: String): Boolean {
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host.endsWith(".local") || host.endsWith(".lan")) {
                return true
            }
            if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("169.254.")) {
                return true
            }
            if (host.startsWith("172.")) {
                val parts = host.split(".")
                if (parts.size >= 2) {
                    val second = parts[1].toIntOrNull()
                    if (second != null && second in 16..31) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return false
    }

    /**
     * Returns standard HTTP headers to inject for privacy & anti-tracking (Do Not Track + Global Privacy Control).
     */
    fun getPrivacyHeaders(context: Context): Map<String, String> {
        if (!isDoNotTrackEnabled(context)) return emptyMap()
        return mapOf(
            "DNT" to "1",
            "Sec-GPC" to "1"
        )
    }

    /**
     * JavaScript payload injected at document_start to set navigator properties:
     * - navigator.doNotTrack = "1"
     * - navigator.globalPrivacyControl = true
     * - Disabling Motion Sensors / WebUSB / WebNFC / WebSerial if blocked by user.
     */
    fun getPrivacyPolyfillScript(context: Context): String {
        val dnt = isDoNotTrackEnabled(context)
        val blockMotion = !isMotionSensorsEnabled(context)
        val blockUsb = getUsbPermission(context) == "block"
        val blockSerial = getSerialPermission(context) == "block"
        val blockNfc = getNfcPermission(context) == "block"
        val blockKbLock = !isKeyboardLockAllowed(context)

        val sb = StringBuilder()
        sb.append("(function() {\n")

        if (dnt) {
            sb.append("""
                try {
                    Object.defineProperty(navigator, 'doNotTrack', { value: '1', configurable: true, writable: true });
                    Object.defineProperty(navigator, 'globalPrivacyControl', { value: true, configurable: true, writable: true });
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        if (blockMotion) {
            sb.append("""
                try {
                    window.addEventListener('devicemotion', function(e) { e.stopImmediatePropagation(); }, true);
                    window.addEventListener('deviceorientation', function(e) { e.stopImmediatePropagation(); }, true);
                    if (window.DeviceMotionEvent) window.DeviceMotionEvent.requestPermission = () => Promise.reject('Denied by Ren browser policy');
                    if (window.DeviceOrientationEvent) window.DeviceOrientationEvent.requestPermission = () => Promise.reject('Denied by Ren browser policy');
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        if (blockUsb) {
            sb.append("""
                try {
                    if (navigator.usb) {
                        navigator.usb.requestDevice = () => Promise.reject(new DOMException('Access to WebUSB is blocked by Ren browser settings.', 'SecurityError'));
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        if (blockSerial) {
            sb.append("""
                try {
                    if (navigator.serial) {
                        navigator.serial.requestPort = () => Promise.reject(new DOMException('Access to WebSerial is blocked by Ren browser settings.', 'SecurityError'));
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        if (blockNfc) {
            sb.append("""
                try {
                    if (window.NDEFReader) {
                        window.NDEFReader = function() { throw new DOMException('Access to WebNFC is blocked by Ren browser settings.', 'SecurityError'); };
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        if (blockKbLock) {
            sb.append("""
                try {
                    if (navigator.keyboard && navigator.keyboard.lock) {
                        navigator.keyboard.lock = () => Promise.reject(new DOMException('Keyboard lock disabled by Ren browser settings.', 'SecurityError'));
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        sb.append("})();")
        return sb.toString()
    }
}
