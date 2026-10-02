package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebSettings
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

data class UserAgentItem(
    val title: String,
    val userAgentString: String,
    val isCustom: Boolean = false
)

object UserAgentManager {

    private const val PREFS_NAME = "ren_browser_prefs"
    private const val KEY_SELECTED_UA = "selected_user_agent_title"
    private const val KEY_DESKTOP_UA = "desktop_mode_user_agent_title"
    private const val KEY_UA_REDUCTION = "user_agent_reduction_enabled"
    private const val KEY_UA_ENABLED = "user_agent_toggle_enabled"
    private const val KEY_CUSTOM_UAS = "custom_user_agents_json"

    val PRESET_USER_AGENTS = listOf(
        UserAgentItem("Default", ""),
        UserAgentItem("Android (Phone)", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"),
        UserAgentItem("Android (Tablet)", "Mozilla/5.0 (Linux; Android 14; Pixel Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"),
        UserAgentItem("Windows (Chrome)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"),
        UserAgentItem("Windows (IE 11)", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko"),
        UserAgentItem("macOS", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"),
        UserAgentItem("iPhone", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1"),
        UserAgentItem("iPad", "Mozilla/5.0 (iPad; CPU OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1"),
        UserAgentItem("Symbian", "Mozilla/5.0 (SymbianOS/9.4; Series60/5.0 NokiaN97-1/20.0.019; Profile/MIDP-2.1 Configuration/CLDC-1.1) AppleWebKit/525 (KHTML, like Gecko) BrowserNG/7.1.18124")
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isUserAgentEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_UA_ENABLED, true)
    }

    fun setUserAgentEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_UA_ENABLED, enabled).apply()
    }

    fun getSelectedTitle(context: Context): String {
        return getPrefs(context).getString(KEY_SELECTED_UA, "Default") ?: "Default"
    }

    fun setSelectedTitle(context: Context, title: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_UA, title).apply()
        if (title.equals("Default", ignoreCase = true)) {
            setUserAgentEnabled(context, false)
        } else {
            setUserAgentEnabled(context, true)
        }
    }

    fun getDesktopModeTitle(context: Context): String {
        return getPrefs(context).getString(KEY_DESKTOP_UA, "Windows (Chrome)") ?: "Windows (Chrome)"
    }

    fun setDesktopModeTitle(context: Context, title: String) {
        getPrefs(context).edit().putString(KEY_DESKTOP_UA, title).apply()
    }

    fun isReductionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_UA_REDUCTION, true)
    }

    fun setReductionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_UA_REDUCTION, enabled).apply()
    }

    fun getCustomUserAgents(context: Context): List<UserAgentItem> {
        val jsonStr = getPrefs(context).getString(KEY_CUSTOM_UAS, null) ?: return emptyList()
        val list = mutableListOf<UserAgentItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "")
                val ua = obj.optString("ua", "")
                if (title.isNotEmpty()) {
                    list.add(UserAgentItem(title, ua, isCustom = true))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addCustomUserAgent(context: Context, title: String, userAgentString: String) {
        val list = getCustomUserAgents(context).toMutableList()
        val index = list.indexOfFirst { it.title.equals(title, ignoreCase = true) }
        val newItem = UserAgentItem(title, userAgentString, isCustom = true)
        if (index >= 0) {
            list[index] = newItem
        } else {
            list.add(newItem)
        }
        saveCustomUserAgentsList(context, list)
    }

    fun deleteCustomUserAgent(context: Context, title: String) {
        val list = getCustomUserAgents(context).filterNot { it.title.equals(title, ignoreCase = true) }
        saveCustomUserAgentsList(context, list)
        if (getSelectedTitle(context).equals(title, ignoreCase = true)) {
            setSelectedTitle(context, "Default")
        }
    }

    private fun saveCustomUserAgentsList(context: Context, list: List<UserAgentItem>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("title", item.title)
                put("ua", item.userAgentString)
            }
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_CUSTOM_UAS, jsonArray.toString()).apply()
    }

    fun getAllUserAgents(context: Context): List<UserAgentItem> {
        val presets = PRESET_USER_AGENTS
        val custom = getCustomUserAgents(context)
        return presets + custom
    }

    fun getUserAgentStringByTitle(context: Context, title: String): String? {
        val all = getAllUserAgents(context)
        val match = all.find { it.title.equals(title, ignoreCase = true) }
        return if (match != null && match.userAgentString.isNotEmpty()) {
            match.userAgentString
        } else {
            null // Default
        }
    }

    fun getDesktopChromeUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    }

    fun applyToWebView(context: Context, webView: WebView, isDesktopSite: Boolean = false) {
        val rawUa = if (isDesktopSite) {
            getUserAgentStringByTitle(context, getDesktopModeTitle(context))
                ?: getUserAgentStringByTitle(context, "Windows (Chrome)")
                ?: getDesktopChromeUserAgent()
        } else if (isUserAgentEnabled(context)) {
            val selected = getSelectedTitle(context)
            getUserAgentStringByTitle(context, selected)
        } else {
            null // Default
        }

        if (rawUa == null) {
            // Restore default system WebView UA
            webView.settings.userAgentString = WebSettings.getDefaultUserAgent(context)
        } else {
            var ua = rawUa
            if (isReductionEnabled(context)) {
                ua = ua.replace(Regex("Chrome/\\d+\\.\\d+\\.\\d+\\.\\d+"), "Chrome/130.0.0.0")
            }
            webView.settings.userAgentString = ua
        }

        webView.settings.apply {
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
    }

    /**
     * Generates a pre-hook script injected at document_start (onPageStarted)
     * to spoof client hints, desktop platform, screen dimensions, matchMedia, and Chrome APIs.
     */
    fun getDesktopModePreHookScript(context: Context, isDesktopSite: Boolean = true, isWebStore: Boolean = false): String {
        val platformStr = if (getDesktopModeTitle(context).contains("mac", ignoreCase = true)) "MacIntel" else "Win32"
        val platformDataStr = if (getDesktopModeTitle(context).contains("mac", ignoreCase = true)) "macOS" else "Windows"

        val sb = StringBuilder()
        sb.append("""
            (function() {
                try {
                    // 1. Spoof navigator.platform & vendor
                    Object.defineProperty(navigator, 'platform', {
                        get: () => '$platformStr',
                        configurable: true
                    });
                    Object.defineProperty(navigator, 'vendor', {
                        get: () => 'Google Inc.',
                        configurable: true
                    });
                    Object.defineProperty(navigator, 'maxTouchPoints', {
                        get: () => 0,
                        configurable: true
                    });
                    Object.defineProperty(navigator, 'deviceMemory', {
                        get: () => 8,
                        configurable: true
                    });
                    Object.defineProperty(navigator, 'hardwareConcurrency', {
                        get: () => 8,
                        configurable: true
                    });

                    // 2. Spoof navigator.userAgentData (Client Hints)
                    const brandsList = [
                        { brand: 'Chromium', version: '130' },
                        { brand: 'Google Chrome', version: '130' },
                        { brand: 'Not?A_Brand', version: '99' }
                    ];

                    const fakeUaData = {
                        brands: brandsList,
                        mobile: false,
                        platform: '$platformDataStr',
                        getHighEntropyValues: function(hints) {
                            return Promise.resolve({
                                architecture: 'x86',
                                bitness: '64',
                                brands: brandsList,
                                mobile: false,
                                model: '',
                                platform: '$platformDataStr',
                                platformVersion: '15.0.0',
                                uaFullVersion: '130.0.6723.70',
                                fullVersionList: brandsList,
                                wow64: false
                            });
                        },
                        toJSON: function() {
                            return { brands: brandsList, mobile: false, platform: '$platformDataStr' };
                        }
                    };

                    Object.defineProperty(navigator, 'userAgentData', {
                        get: () => fakeUaData,
                        configurable: true
                    });

                    // 3. Spoof Screen & Window Dimensions for desktop layout resolution
                    try {
                        Object.defineProperty(window.screen, 'width', { get: () => 1920, configurable: true });
                        Object.defineProperty(window.screen, 'availWidth', { get: () => 1920, configurable: true });
                        Object.defineProperty(window.screen, 'height', { get: () => 1080, configurable: true });
                        Object.defineProperty(window.screen, 'availHeight', { get: () => 1040, configurable: true });
                        Object.defineProperty(window.screen, 'colorDepth', { get: () => 24, configurable: true });
                        Object.defineProperty(window.screen, 'pixelDepth', { get: () => 24, configurable: true });
                        Object.defineProperty(window, 'outerWidth', { get: () => 1280, configurable: true });
                        Object.defineProperty(window, 'outerHeight', { get: () => 800, configurable: true });
                    } catch(e) {}

                    // 4. Spoof matchMedia for desktop hover/pointer CSS features
                    try {
                        const origMatchMedia = window.matchMedia ? window.matchMedia.bind(window) : null;
                        if (origMatchMedia) {
                            window.matchMedia = function(query) {
                                const mql = origMatchMedia(query);
                                const q = (query || '').toLowerCase();
                                if (q.includes('hover: hover') || q.includes('pointer: fine') || q.includes('hover:hover') || q.includes('pointer:fine')) {
                                    return Object.create(mql, {
                                        matches: { get: () => true, configurable: true }
                                    });
                                }
                                if (q.includes('pointer: coarse') || q.includes('hover: none') || q.includes('pointer:coarse') || q.includes('hover:none')) {
                                    return Object.create(mql, {
                                        matches: { get: () => false, configurable: true }
                                    });
                                }
                                return mql;
                            };
                        }
                    } catch(e) {}

                    // 5. Desktop Viewport Enforcement
                    const targetViewport = 'width=1280, initial-scale=0.35, minimum-scale=0.1, maximum-scale=5.0, user-scalable=yes';
                    function enforceDesktopViewport() {
                        try {
                            let vp = document.querySelector('meta[name="viewport"]');
                            if (!vp) {
                                vp = document.createElement('meta');
                                vp.name = 'viewport';
                                (document.head || document.documentElement).appendChild(vp);
                            }
                            if (vp && vp.getAttribute('content') !== targetViewport) {
                                vp.setAttribute('content', targetViewport);
                            }
                        } catch(e) {}
                    }

                    if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', enforceDesktopViewport, { once: true });
                    } else {
                        enforceDesktopViewport();
                    }

                    try {
                        const vpObserver = new MutationObserver(function(mutations) {
                            for (let i = 0; i < mutations.length; i++) {
                                const added = mutations[i].addedNodes;
                                for (let j = 0; j < added.length; j++) {
                                    const node = added[j];
                                    if (node.nodeName === 'META' && (node.getAttribute('name') || '').toLowerCase() === 'viewport') {
                                        if (node.getAttribute('content') !== targetViewport) {
                                            node.setAttribute('content', targetViewport);
                                        }
                                    }
                                }
                            }
                        });
                        vpObserver.observe(document.documentElement || document, { childList: true, subtree: true });
                    } catch(e) {}

                    // 6. Spoof window.chrome and chrome APIs
                    window.chrome = window.chrome || {};
                    window.chrome.app = window.chrome.app || {
                        isInstalled: false,
                        InstallState: { DISABLED: 'disabled', INSTALLED: 'installed', NOT_INSTALLED: 'not_installed' },
                        RunningState: { CANNOT_RUN: 'cannot_run', READY_TO_RUN: 'ready_to_run', RUNNING: 'running' },
                        getDetails: function() { return null; },
                        getIsInstalled: function() { return false; },
                        runningState: function() { return 'cannot_run'; }
                    };

                    window.chrome.runtime = window.chrome.runtime || {
                        id: undefined,
                        onMessage: { addListener: function() {}, removeListener: function() {} },
                        sendMessage: function() {}
                    };
                } catch(e) {
                    console.error('Ren Desktop Mode PreHook error:', e);
                }
            })();
        """.trimIndent())
        return sb.toString()
    }

    /**
     * Post-hook script injected at document_end / onPageFinished to ensure viewport and desktop layout
     * stay locked in place even after SPA dynamic rendering.
     */
    fun getDesktopModePostHookScript(): String {
        return """
            (function() {
                try {
                    const targetViewport = 'width=1280, initial-scale=0.35, minimum-scale=0.1, maximum-scale=5.0, user-scalable=yes';
                    let vp = document.querySelector('meta[name="viewport"]');
                    if (vp && vp.getAttribute('content') !== targetViewport) {
                        vp.setAttribute('content', targetViewport);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }
}
