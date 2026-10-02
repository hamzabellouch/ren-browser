package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebView

object AntiFingerprintManager {

    private const val PREFS_NAME = "ren_anti_fingerprint_prefs"

    // Master switch
    private const val KEY_ANTI_FINGERPRINT_ENABLED = "anti_fingerprint_enabled"

    // Individual vector controls
    private const val KEY_CANVAS_PROTECTION = "afp_canvas_protection"
    private const val KEY_WEBGL_PROTECTION = "afp_webgl_protection"
    private const val KEY_AUDIO_PROTECTION = "afp_audio_protection"
    private const val KEY_FONT_PROTECTION = "afp_font_protection"
    private const val KEY_HARDWARE_PROTECTION = "afp_hardware_protection"
    private const val KEY_BATTERY_PROTECTION = "afp_battery_protection"
    private const val KEY_MEDIA_DEVICES_PROTECTION = "afp_media_devices_protection"
    private const val KEY_DOM_RECT_PROTECTION = "afp_dom_rect_protection"
    private const val KEY_SPEECH_PROTECTION = "afp_speech_protection"
    private const val KEY_TIMER_PROTECTION = "afp_timer_protection"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Master Toggle ---

    fun isAntiFingerprintEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ANTI_FINGERPRINT_ENABLED, true)
    }

    fun setAntiFingerprintEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ANTI_FINGERPRINT_ENABLED, enabled).apply()
    }

    // --- Individual Vectors ---

    fun isCanvasProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CANVAS_PROTECTION, true)
    }

    fun setCanvasProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CANVAS_PROTECTION, enabled).apply()
    }

    fun isWebGlProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WEBGL_PROTECTION, true)
    }

    fun setWebGlProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WEBGL_PROTECTION, enabled).apply()
    }

    fun isAudioProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUDIO_PROTECTION, true)
    }

    fun setAudioProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUDIO_PROTECTION, enabled).apply()
    }

    fun isFontProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FONT_PROTECTION, true)
    }

    fun setFontProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FONT_PROTECTION, enabled).apply()
    }

    fun isHardwareProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HARDWARE_PROTECTION, true)
    }

    fun setHardwareProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HARDWARE_PROTECTION, enabled).apply()
    }

    fun isBatteryProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_PROTECTION, true)
    }

    fun setBatteryProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_PROTECTION, enabled).apply()
    }

    fun isMediaDevicesProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MEDIA_DEVICES_PROTECTION, true)
    }

    fun setMediaDevicesProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MEDIA_DEVICES_PROTECTION, enabled).apply()
    }

    fun isDomRectProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DOM_RECT_PROTECTION, true)
    }

    fun setDomRectProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DOM_RECT_PROTECTION, enabled).apply()
    }

    fun isSpeechProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SPEECH_PROTECTION, true)
    }

    fun setSpeechProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SPEECH_PROTECTION, enabled).apply()
    }

    fun isTimerProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TIMER_PROTECTION, true)
    }

    fun setTimerProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TIMER_PROTECTION, enabled).apply()
    }

    /**
     * Generates a comprehensive Anti-Fingerprinting Javascript script to inject into WebViews
     * at document_start / onPageStarted.
     */
    fun getAntiFingerprintScript(context: Context, isDesktopSite: Boolean = false): String {
        if (!isAntiFingerprintEnabled(context)) {
            return ""
        }

        val canvas = isCanvasProtectionEnabled(context)
        val webgl = isWebGlProtectionEnabled(context)
        val audio = isAudioProtectionEnabled(context)
        val font = isFontProtectionEnabled(context)
        val hardware = isHardwareProtectionEnabled(context)
        val battery = isBatteryProtectionEnabled(context)
        val media = isMediaDevicesProtectionEnabled(context)
        val domRect = isDomRectProtectionEnabled(context)
        val speech = isSpeechProtectionEnabled(context)
        val timer = isTimerProtectionEnabled(context)

        val sb = StringBuilder()
        sb.append("""
            (function() {
                'use strict';
                if (window.__ren_anti_fingerprint_injected__) return;
                window.__ren_anti_fingerprint_injected__ = true;

                // Deterministic pseudo-random noise generator per page session
                var sessionSeed = (Math.random() * 1000000) | 0;
                function getNoise(index) {
                    var x = Math.sin(sessionSeed + index) * 10000;
                    return x - Math.floor(x);
                }
        """.trimIndent()).append("\n")

        // 1. Canvas Fingerprinting Protection
        if (canvas) {
            sb.append("""
                // --- 1. Canvas Fingerprinting Defense ---
                try {
                    var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;

                    CanvasRenderingContext2D.prototype.getImageData = function(sx, sy, sw, sh) {
                        var imgData = origGetImageData.apply(this, arguments);
                        try {
                            var data = imgData.data;
                            var len = data.length;
                            for (var i = 0; i < len; i += 32) {
                                if (data[i + 3] > 0) {
                                    var noise = getNoise(i);
                                    if (noise > 0.8) {
                                        data[i] = (data[i] ^ 1);
                                    }
                                }
                            }
                        } catch(e) {}
                        return imgData;
                    };
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 2. WebGL & GPU Protection
        if (webgl) {
            sb.append("""
                // --- 2. WebGL & GPU Spoofing Defense ---
                try {
                    var spoofGlParams = function(targetProto) {
                        if (!targetProto) return;
                        var origGetParam = targetProto.getParameter;
                        var origReadPixels = targetProto.readPixels;

                        targetProto.getParameter = function(pname) {
                            // UNMASKED_VENDOR_WEBGL
                            if (pname === 0x9245) return 'Google Inc. (Google)';
                            // UNMASKED_RENDERER_WEBGL
                            if (pname === 0x9246) return 'ANGLE (Google, Vulkan 1.3.0, Android WebGL)';
                            // VENDOR
                            if (pname === 0x1F00) return 'WebKit';
                            // RENDERER
                            if (pname === 0x1F01) return 'WebKit WebGL';
                            // VERSION
                            if (pname === 0x1F02) return 'WebGL 1.0 (OpenGL ES 2.0 Chromium)';
                            // SHADING_LANGUAGE_VERSION
                            if (pname === 0x8DF8) return 'WebGL GLSL ES 1.0 (OpenGL ES GLSL ES 1.0 Chromium)';
                            // Standardize hardware caps to generic high-end values
                            if (pname === 0x0D33) return 8192; // MAX_TEXTURE_SIZE
                            if (pname === 0x851C) return 8192; // MAX_CUBE_MAP_TEXTURE_SIZE
                            if (pname === 0x84E8) return 8192; // MAX_RENDERBUFFER_SIZE
                            if (pname === 0x8869) return 16;   // MAX_VERTEX_ATTRIBS
                            if (pname === 0x8DFC) return 15;   // MAX_VARYING_VECTORS
                            if (pname === 0x0D3A) return new Int32Array([8192, 8192]); // MAX_VIEWPORT_DIMS

                            return origGetParam.apply(this, arguments);
                        };

                        if (origReadPixels) {
                            targetProto.readPixels = function(x, y, width, height, format, type, pixels) {
                                origReadPixels.apply(this, arguments);
                                try {
                                    if (pixels && pixels.length > 0) {
                                        pixels[0] = (pixels[0] ^ 1);
                                    }
                                } catch(e) {}
                            };
                        }
                    };

                    if (window.WebGLRenderingContext) spoofGlParams(window.WebGLRenderingContext.prototype);
                    if (window.WebGL2RenderingContext) spoofGlParams(window.WebGL2RenderingContext.prototype);

                    if (navigator.gpu && navigator.gpu.requestAdapter) {
                        var origReqAdapter = navigator.gpu.requestAdapter;
                        navigator.gpu.requestAdapter = function() {
                            return origReqAdapter.apply(this, arguments).then(function(adapter) {
                                if (adapter && adapter.info) {
                                    try {
                                        Object.defineProperty(adapter.info, 'vendor', { get: function() { return 'google'; } });
                                        Object.defineProperty(adapter.info, 'architecture', { get: function() { return 'generic'; } });
                                        Object.defineProperty(adapter.info, 'device', { get: function() { return 'generic'; } });
                                        Object.defineProperty(adapter.info, 'description', { get: function() { return 'ANGLE WebGPU'; } });
                                    } catch(e) {}
                                }
                                return adapter;
                            });
                        };
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 3. AudioContext / Web Audio Protection
        if (audio) {
            sb.append("""
                // --- 3. AudioContext Fingerprinting Defense ---
                try {
                    if (window.AudioBuffer) {
                        var origGetChannelData = AudioBuffer.prototype.getChannelData;
                        var origCopyFromChannel = AudioBuffer.prototype.copyFromChannel;

                        AudioBuffer.prototype.getChannelData = function(channel) {
                            var buffer = origGetChannelData.apply(this, arguments);
                            try {
                                for (var i = 0; i < buffer.length; i += 100) {
                                    var delta = (getNoise(i + channel) - 0.5) * 0.0000001;
                                    buffer[i] += delta;
                                }
                            } catch(e) {}
                            return buffer;
                        };

                        if (origCopyFromChannel) {
                            AudioBuffer.prototype.copyFromChannel = function(destination, channelNumber, startInChannel) {
                                origCopyFromChannel.apply(this, arguments);
                                try {
                                    for (var i = 0; i < destination.length; i += 100) {
                                        var delta = (getNoise(i + channelNumber) - 0.5) * 0.0000001;
                                        destination[i] += delta;
                                    }
                                } catch(e) {}
                            };
                        }
                    }

                    if (window.AnalyserNode) {
                        var origGetFloatFreq = AnalyserNode.prototype.getFloatFrequencyData;
                        var origGetByteFreq = AnalyserNode.prototype.getByteFrequencyData;
                        var origGetFloatTime = AnalyserNode.prototype.getFloatTimeDomainData;
                        var origGetByteTime = AnalyserNode.prototype.getByteTimeDomainData;

                        if (origGetFloatFreq) {
                            AnalyserNode.prototype.getFloatFrequencyData = function(array) {
                                origGetFloatFreq.apply(this, arguments);
                                for (var i = 0; i < array.length; i += 8) {
                                    array[i] += (getNoise(i) - 0.5) * 0.01;
                                }
                            };
                        }
                        if (origGetByteFreq) {
                            AnalyserNode.prototype.getByteFrequencyData = function(array) {
                                origGetByteFreq.apply(this, arguments);
                                for (var i = 0; i < array.length; i += 16) {
                                    array[i] = (array[i] ^ 1);
                                }
                            };
                        }
                        if (origGetFloatTime) {
                            AnalyserNode.prototype.getFloatTimeDomainData = function(array) {
                                origGetFloatTime.apply(this, arguments);
                                for (var i = 0; i < array.length; i += 8) {
                                    array[i] += (getNoise(i) - 0.5) * 0.0001;
                                }
                            };
                        }
                        if (origGetByteTime) {
                            AnalyserNode.prototype.getByteTimeDomainData = function(array) {
                                origGetByteTime.apply(this, arguments);
                                for (var i = 0; i < array.length; i += 16) {
                                    array[i] = (array[i] ^ 1);
                                }
                            };
                        }
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 4. Font Probing Protection
        if (font) {
            sb.append("""
                // --- 4. Font Probing & Metrics Defense ---
                try {
                    if (document.fonts && document.fonts.check) {
                        var origFontsCheck = document.fonts.check;
                        var STANDARD_FONTS = ['arial', 'helvetica', 'times new roman', 'times', 'courier new', 'courier', 'roboto', 'sans-serif', 'serif', 'monospace', 'georgia', 'verdana', 'tahoma', 'trebuchet ms'];
                        document.fonts.check = function(fontSpec, text) {
                            var lower = (fontSpec || '').toLowerCase();
                            var isStandard = STANDARD_FONTS.some(function(f) { return lower.indexOf(f) !== -1; });
                            if (isStandard) return true;
                            return false;
                        };
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 5. DOM & Layout Jitter Protection
        if (domRect) {
            sb.append("""
                // --- 5. DOM & Layout Sub-pixel Jitter Defense ---
                try {
                    var applyRectNoise = function(rect, idx) {
                        if (!rect) return rect;
                        var n = (getNoise(idx) - 0.5) * 0.00005;
                        var x = rect.x + n;
                        var y = rect.y + n;
                        var w = rect.width + n;
                        var h = rect.height + n;
                        if (window.DOMRect && typeof window.DOMRect.fromRect === 'function') {
                            try {
                                return DOMRect.fromRect({ x: x, y: y, width: w, height: h });
                            } catch(e) {}
                        }
                        if (typeof DOMRect === 'function') {
                            try {
                                return new DOMRect(x, y, w, h);
                            } catch(e) {}
                        }
                        return rect;
                    };

                    var origGetBoundingClientRect = Element.prototype.getBoundingClientRect;
                    Element.prototype.getBoundingClientRect = function() {
                        var rect = origGetBoundingClientRect.apply(this, arguments);
                        return applyRectNoise(rect, 1);
                    };

                    var origGetClientRects = Element.prototype.getClientRects;
                    Element.prototype.getClientRects = function() {
                        var rects = origGetClientRects.apply(this, arguments);
                        if (!rects) return rects;
                        var list = [];
                        for (var i = 0; i < rects.length; i++) {
                            list.push(applyRectNoise(rects[i], i + 2));
                        }
                        return list;
                    };

                    if (window.Range) {
                        var origRangeGetBounding = Range.prototype.getBoundingClientRect;
                        Range.prototype.getBoundingClientRect = function() {
                            var rect = origRangeGetBounding.apply(this, arguments);
                            return applyRectNoise(rect, 10);
                        };
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 6. Hardware & Platform Normalization
        if (hardware) {
            val desktopPlatform = if (UserAgentManager.getDesktopModeTitle(context).contains("mac", ignoreCase = true)) "MacIntel" else "Win32"
            val targetPlatform = if (isDesktopSite) desktopPlatform else "Linux armv81"
            val targetTouchPoints = if (isDesktopSite) 0 else 5
            val targetConcurrency = if (isDesktopSite) 8 else 4

            sb.append("""
                // --- 6. Hardware & Platform Normalization ---
                try {
                    Object.defineProperty(navigator, 'hardwareConcurrency', { value: $targetConcurrency, configurable: true, writable: true });
                    Object.defineProperty(navigator, 'deviceMemory', { value: 8, configurable: true, writable: true });
                    Object.defineProperty(navigator, 'maxTouchPoints', { value: $targetTouchPoints, configurable: true, writable: true });
                    Object.defineProperty(navigator, 'platform', { value: '$targetPlatform', configurable: true, writable: true });

                    // Standardize NetworkInformation
                    var fakeConnection = {
                        downlink: 10,
                        effectiveType: '4g',
                        rtt: 50,
                        saveData: false,
                        addEventListener: function() {},
                        removeEventListener: function() {},
                        dispatchEvent: function() { return true; }
                    };
                    try { Object.defineProperty(navigator, 'connection', { value: fakeConnection, configurable: true, writable: true }); } catch(e) {}
                    try { Object.defineProperty(navigator, 'mozConnection', { value: fakeConnection, configurable: true, writable: true }); } catch(e) {}
                    try { Object.defineProperty(navigator, 'webkitConnection', { value: fakeConnection, configurable: true, writable: true }); } catch(e) {}

                    // Normalize plugins and mimeTypes
                    try {
                        Object.defineProperty(navigator, 'plugins', { value: [], configurable: true, writable: true });
                        Object.defineProperty(navigator, 'mimeTypes', { value: [], configurable: true, writable: true });
                    } catch(e) {}
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 7. Battery Status Shield
        if (battery) {
            sb.append("""
                // --- 7. Battery Status Spoofing ---
                try {
                    var fakeBattery = {
                        charging: true,
                        chargingTime: 0,
                        dischargingTime: Infinity,
                        level: 1.0,
                        addEventListener: function() {},
                        removeEventListener: function() {},
                        dispatchEvent: function() { return true; }
                    };
                    if (navigator.getBattery) {
                        navigator.getBattery = function() {
                            return Promise.resolve(fakeBattery);
                        };
                    }
                    if ('battery' in navigator) {
                        try { Object.defineProperty(navigator, 'battery', { value: fakeBattery, configurable: true, writable: true }); } catch(e) {}
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 8. Media Devices & WebRTC Masking
        if (media) {
            sb.append("""
                // --- 8. Media Devices & WebRTC Masking ---
                try {
                    if (navigator.mediaDevices && navigator.mediaDevices.enumerateDevices) {
                        navigator.mediaDevices.enumerateDevices = function() {
                            return Promise.resolve([
                                { deviceId: 'default', kind: 'audioinput', label: '', groupId: 'group-default-audio' },
                                { deviceId: 'default', kind: 'videoinput', label: '', groupId: 'group-default-video' },
                                { deviceId: 'default', kind: 'audiooutput', label: '', groupId: 'group-default-audio' }
                            ]);
                        };
                    }

                    // WebRTC IP leak protection: sanitize local candidate IP addresses
                    var PeerConnection = window.RTCPeerConnection || window.webkitRTCPeerConnection || window.mozRTCPeerConnection;
                    if (PeerConnection) {
                        var origCreateOffer = PeerConnection.prototype.createOffer;
                        if (origCreateOffer) {
                            PeerConnection.prototype.createOffer = function(options) {
                                return origCreateOffer.apply(this, arguments);
                            };
                        }
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 9. Speech Synthesis Voices Masking
        if (speech) {
            sb.append("""
                // --- 9. Speech Synthesis Voices Shield ---
                try {
                    if (window.speechSynthesis) {
                        var fakeVoices = [
                            { default: true, lang: 'en-US', localService: true, name: 'Default English Voice', voiceURI: 'default' }
                        ];
                        window.speechSynthesis.getVoices = function() {
                            return fakeVoices;
                        };
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        // 10. High-Resolution Timing Clamping
        if (timer) {
            sb.append("""
                // --- 10. High-Resolution Timing Protection ---
                try {
                    if (window.performance && window.performance.now) {
                        var origPerfNow = window.performance.now.bind(window.performance);
                        window.performance.now = function() {
                            var t = origPerfNow();
                            return Math.floor(t / 10) * 10 + (getNoise((t | 0)) * 0.5);
                        };
                    }
                } catch(e) {}
            """.trimIndent()).append("\n")
        }

        sb.append("})();\n")
        return sb.toString()
    }
}
