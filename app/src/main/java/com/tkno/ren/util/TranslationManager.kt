package com.tkno.ren.util

import android.webkit.CookieManager
import android.webkit.WebView

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false
)

object TranslationManager {

    val SUPPORTED_LANGUAGES = listOf(
        LanguageItem("ar", "Arabic", "العربية", "🇸🇦", isRtl = true),
        LanguageItem("en", "English", "English", "🇬🇧", isRtl = false),
        LanguageItem("fr", "French", "Français", "🇫🇷", isRtl = false),
        LanguageItem("es", "Spanish", "Español", "🇪🇸", isRtl = false),
        LanguageItem("de", "German", "Deutsch", "🇩🇪", isRtl = false),
        LanguageItem("tr", "Turkish", "Türkçe", "🇹🇷", isRtl = false),
        LanguageItem("ru", "Russian", "Русский", "🇷🇺", isRtl = false),
        LanguageItem("it", "Italian", "Italiano", "🇮🇹", isRtl = false),
        LanguageItem("zh-CN", "Chinese", "中文", "🇨🇳", isRtl = false),
        LanguageItem("ja", "Japanese", "日本語", "🇯🇵", isRtl = false),
        LanguageItem("ko", "Korean", "한국어", "🇰🇷", isRtl = false),
        LanguageItem("pt", "Portuguese", "Português", "🇵🇹", isRtl = false),
        LanguageItem("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", isRtl = false),
        LanguageItem("hi", "Hindi", "हिन्दी", "🇮🇳", isRtl = false),
        LanguageItem("fa", "Persian", "فارسی", "🇮🇷", isRtl = true),
        LanguageItem("ur", "Urdu", "اردو", "🇵🇰", isRtl = true)
    )

    fun translatePage(webView: WebView, targetLang: String) {
        val currentUrl = webView.url ?: return
        if (currentUrl.startsWith("about:") || currentUrl.startsWith("chrome:") || currentUrl.startsWith("file:")) {
            return
        }

        val langObj = SUPPORTED_LANGUAGES.find { it.code.equals(targetLang, ignoreCase = true) }
        val isRtl = langObj?.isRtl == true || targetLang == "ar" || targetLang == "fa" || targetLang == "he" || targetLang == "ur"

        // Set Google Translate Cookie immediately across domains
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setCookie(currentUrl, "googtrans=/auto/$targetLang; path=/")
        } catch (e: Exception) {
            // Ignored
        }

        val script = """
            (function() {
                var targetLang = '$targetLang';
                var isRtl = $isRtl;

                // 1. Instant cookie injection across host and root domain
                var hostname = window.location.hostname;
                var domainParts = hostname.split('.');
                var domain = domainParts.length > 1 ? '.' + domainParts.slice(-2).join('.') : hostname;
                document.cookie = 'googtrans=/auto/' + targetLang + '; path=/;';
                document.cookie = 'googtrans=/auto/' + targetLang + '; domain=' + domain + '; path=/;';

                // 2. Hide Google banner / tooltips / popups without breaking viewport dimensions
                var cleanStyleId = 'ren-translate-clean-style';
                var existingCleanStyle = document.getElementById(cleanStyleId);
                if (!existingCleanStyle) {
                    var style = document.createElement('style');
                    style.id = cleanStyleId;
                    style.innerHTML = 
                        '.goog-te-banner-frame, .skiptranslate, #goog-gt-tt, .goog-te-balloon-frame, .goog-tooltip, .goog-tooltip-skiptranslate { ' +
                        '  display: none !important; visibility: hidden !important; width: 0 !important; height: 0 !important; ' +
                        '  max-width: 0 !important; max-height: 0 !important; position: absolute !important; top: -9999px !important; ' +
                        '  left: -9999px !important; overflow: hidden !important; border: none !important; ' +
                        '} ' +
                        'body { top: 0px !important; position: static !important; } ' +
                        'html, body { -webkit-text-size-adjust: 100% !important; text-size-adjust: 100% !important; } ' +
                        '.goog-text-highlight { background: none !important; box-shadow: none !important; }';
                    (document.head || document.documentElement).appendChild(style);
                }

                // 3. Direction adjustment without layout distortion or zooming
                var rtlStyleId = 'ren-translate-rtl-style';
                var existingRtlStyle = document.getElementById(rtlStyleId);
                if (isRtl) {
                    document.documentElement.setAttribute('dir', 'rtl');
                    document.body.setAttribute('dir', 'rtl');
                    if (!existingRtlStyle) {
                        var rtlStyle = document.createElement('style');
                        rtlStyle.id = rtlStyleId;
                        rtlStyle.innerHTML = 
                            'html[dir="rtl"], body[dir="rtl"] { ' +
                            '  direction: rtl !important; text-align: right !important; ' +
                            '  -webkit-text-size-adjust: 100% !important; text-size-adjust: 100% !important; ' +
                            '}';
                        (document.head || document.documentElement).appendChild(rtlStyle);
                    }
                } else {
                    document.documentElement.setAttribute('dir', 'ltr');
                    document.body.setAttribute('dir', 'ltr');
                    if (existingRtlStyle) {
                        existingRtlStyle.remove();
                    }
                }

                // 4. Helper function to apply translation with seamless auto-revert if already translated
                function applyTranslationToCombo(combo) {
                    if (!combo) return false;
                    var currentVal = combo.value;
                    if (currentVal === targetLang) {
                        return true;
                    }

                    // If a different translation was already active, reset to original first, then switch to targetLang
                    if (currentVal && currentVal !== '' && currentVal !== targetLang) {
                        combo.value = '';
                        combo.dispatchEvent(new Event('change'));
                        setTimeout(function() {
                            combo.value = targetLang;
                            combo.dispatchEvent(new Event('change'));
                        }, 60);
                    } else {
                        combo.value = targetLang;
                        combo.dispatchEvent(new Event('change'));
                    }
                    return true;
                }

                var teCombo = document.querySelector('.goog-te-combo');
                if (applyTranslationToCombo(teCombo)) {
                    return;
                }

                // Fast polling observer until Google Translate combo is mounted
                var attempts = 0;
                var pollInterval = setInterval(function() {
                    attempts++;
                    var c = document.querySelector('.goog-te-combo');
                    if (applyTranslationToCombo(c) || attempts > 60) {
                        clearInterval(pollInterval);
                    }
                }, 25);

                // 5. Mount hidden translate container if missing
                if (!document.getElementById('google-translate-element')) {
                    var div = document.createElement('div');
                    div.id = 'google-translate-element';
                    div.style.display = 'none';
                    (document.body || document.documentElement).appendChild(div);
                }

                // 6. Setup initialization callback with all languages included
                window.googleTranslateElementInit = function() {
                    try {
                        new google.translate.TranslateElement({
                            pageLanguage: 'auto',
                            layout: google.translate.TranslateElement.InlineLayout.SIMPLE,
                            autoDisplay: false
                        }, 'google-translate-element');
                    } catch(e) {}
                    var c = document.querySelector('.goog-te-combo');
                    applyTranslationToCombo(c);
                };

                // 7. Inject Google Translate script if missing
                var scriptId = 'google-translate-script';
                if (!document.getElementById(scriptId)) {
                    var s = document.createElement('script');
                    s.id = scriptId;
                    s.type = 'text/javascript';
                    s.async = true;
                    s.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
                    (document.head || document.documentElement).appendChild(s);
                } else {
                    if (typeof window.googleTranslateElementInit === 'function') {
                        window.googleTranslateElementInit();
                    }
                }
            })();
        """.trimIndent()

        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }

    fun restoreOriginal(webView: WebView) {
        val currentUrl = webView.url ?: return
        if (currentUrl.startsWith("about:") || currentUrl.startsWith("chrome:") || currentUrl.startsWith("file:")) {
            return
        }

        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie(currentUrl, "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/")
        } catch (e: Exception) {
            // Ignored
        }

        val script = """
            (function() {
                var hostname = window.location.hostname;
                var domainParts = hostname.split('.');
                var domain = domainParts.length > 1 ? '.' + domainParts.slice(-2).join('.') : hostname;
                document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
                document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; domain=' + domain + '; path=/;';

                document.documentElement.removeAttribute('dir');
                document.body.removeAttribute('dir');
                document.documentElement.style.direction = '';
                document.body.style.direction = '';

                var rtlStyle = document.getElementById('ren-translate-rtl-style');
                if (rtlStyle) {
                    rtlStyle.remove();
                }

                var teCombo = document.querySelector('.goog-te-combo');
                if (teCombo) {
                    teCombo.value = '';
                    teCombo.dispatchEvent(new Event('change'));
                } else {
                    window.location.reload();
                }
            })();
        """.trimIndent()

        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }
}
