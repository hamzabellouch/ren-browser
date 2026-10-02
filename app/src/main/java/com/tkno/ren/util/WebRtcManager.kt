package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences

object WebRtcManager {

    private const val PREFS_NAME = "ren_webrtc_prefs"

    // Master switch
    private const val KEY_WEBRTC_BLOCK_ENABLED = "webrtc_block_enabled"

    // Granular protection vectors
    private const val KEY_BLOCK_PEER_CONNECTION = "webrtc_block_peer_connection"
    private const val KEY_PREVENT_IP_LEAKS = "webrtc_prevent_ip_leaks"
    private const val KEY_BLOCK_STUN_TURN = "webrtc_block_stun_turn"
    private const val KEY_BLOCK_DATA_CHANNELS = "webrtc_block_data_channels"
    private const val KEY_RESTRICT_MEDIA_DEVICES = "webrtc_restrict_media_devices"
    private const val KEY_TOR_ISOLATION = "webrtc_tor_isolation"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Master Toggle ---

    fun isWebRtcBlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WEBRTC_BLOCK_ENABLED, true)
    }

    fun setWebRtcBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WEBRTC_BLOCK_ENABLED, enabled).apply()
    }

    // --- Granular Vectors ---

    fun isPeerConnectionBlocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_PEER_CONNECTION, true)
    }

    fun setPeerConnectionBlocked(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_PEER_CONNECTION, enabled).apply()
    }

    fun isIpLeakProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PREVENT_IP_LEAKS, true)
    }

    fun setIpLeakProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PREVENT_IP_LEAKS, enabled).apply()
    }

    fun isStunTurnBlocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_STUN_TURN, true)
    }

    fun setStunTurnBlocked(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_STUN_TURN, enabled).apply()
    }

    fun isDataChannelsBlocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_DATA_CHANNELS, true)
    }

    fun setDataChannelsBlocked(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_DATA_CHANNELS, enabled).apply()
    }

    fun isMediaDevicesRestricted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_RESTRICT_MEDIA_DEVICES, true)
    }

    fun setMediaDevicesRestricted(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_RESTRICT_MEDIA_DEVICES, enabled).apply()
    }

    fun isTorIsolationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TOR_ISOLATION, true)
    }

    fun setTorIsolationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TOR_ISOLATION, enabled).apply()
    }

    /**
     * Generates a JavaScript payload injected into WebViews to neutralize WebRTC IP leaks
     * and restrict/block WebRTC protocols according to configured vectors.
     */
    fun getWebRtcBlockScript(context: Context): String {
        if (!isWebRtcBlockEnabled(context)) {
            return ""
        }

        val blockPeerConn = isPeerConnectionBlocked(context)
        val preventIpLeaks = isIpLeakProtectionEnabled(context)
        val blockStunTurn = isStunTurnBlocked(context)
        val blockDataChannels = isDataChannelsBlocked(context)
        val restrictMedia = isMediaDevicesRestricted(context)

        return """
            (function() {
                'use strict';
                if (window.__ren_webrtc_shield_injected__) return;
                window.__ren_webrtc_shield_injected__ = true;

                var blockPeerConn = $blockPeerConn;
                var preventIpLeaks = $preventIpLeaks;
                var blockStunTurn = $blockStunTurn;
                var blockDataChannels = $blockDataChannels;
                var restrictMedia = $restrictMedia;

                // 1. Complete PeerConnection Blocking
                if (blockPeerConn) {
                    var noopPeerConnection = function() {
                        throw new DOMException('WebRTC RTCPeerConnection is disabled by Ren Privacy Shield', 'NotSupportedError');
                    };
                    try {
                        window.RTCPeerConnection = noopPeerConnection;
                        window.webkitRTCPeerConnection = noopPeerConnection;
                        window.mozRTCPeerConnection = noopPeerConnection;
                        window.RTCSessionDescription = function() {};
                        window.RTCIceCandidate = function() {};
                    } catch (e) {}
                    return;
                }

                // 2. Granular Protection: Intercept RTCPeerConnection
                var OriginalPeerConnection = window.RTCPeerConnection || window.webkitRTCPeerConnection || window.mozRTCPeerConnection;
                if (OriginalPeerConnection) {
                    var CustomPeerConnection = function(config, constraints) {
                        var sanitizedConfig = config || {};
                        
                        // Block STUN / TURN Servers if enabled
                        if (blockStunTurn && sanitizedConfig.iceServers) {
                            sanitizedConfig.iceServers = [];
                        }

                        var pc = new OriginalPeerConnection(sanitizedConfig, constraints);

                        // Block Data Channels
                        if (blockDataChannels) {
                            pc.createDataChannel = function() {
                                throw new DOMException('WebRTC Data Channels are blocked', 'NotSupportedError');
                            };
                        }

                        // Prevent IP Leaks by stripping Host & Public candidates from SDP
                        if (preventIpLeaks) {
                            var originalCreateOffer = pc.createOffer.bind(pc);
                            pc.createOffer = function(options) {
                                return originalCreateOffer(options).then(function(offer) {
                                    if (offer && offer.sdp) {
                                        offer.sdp = offer.sdp.replace(/a=candidate:.*?(typ host|typ srflx).*?\r\n/g, '');
                                    }
                                    return offer;
                                });
                            };

                            var originalCreateAnswer = pc.createAnswer.bind(pc);
                            pc.createAnswer = function(options) {
                                return originalCreateAnswer(options).then(function(answer) {
                                    if (answer && answer.sdp) {
                                        answer.sdp = answer.sdp.replace(/a=candidate:.*?(typ host|typ srflx).*?\r\n/g, '');
                                    }
                                    return answer;
                                });
                            };

                            var originalAddEventListener = pc.addEventListener.bind(pc);
                            pc.addEventListener = function(type, listener, options) {
                                if (type === 'icecandidate') {
                                    var wrappedListener = function(event) {
                                        if (event && event.candidate) {
                                            var cand = event.candidate.candidate || '';
                                            // Suppress local and public IP candidates
                                            if (cand.indexOf('typ host') !== -1 || cand.indexOf('typ srflx') !== -1) {
                                                return;
                                            }
                                        }
                                        listener(event);
                                    };
                                    return originalAddEventListener(type, wrappedListener, options);
                                }
                                return originalAddEventListener(type, listener, options);
                            };
                        }

                        return pc;
                    };

                    CustomPeerConnection.prototype = OriginalPeerConnection.prototype;
                    try {
                        window.RTCPeerConnection = CustomPeerConnection;
                        window.webkitRTCPeerConnection = CustomPeerConnection;
                        window.mozRTCPeerConnection = CustomPeerConnection;
                    } catch (e) {}
                }

                // 3. Media Devices Protection
                if (restrictMedia && navigator.mediaDevices && navigator.mediaDevices.enumerateDevices) {
                    var origEnumerate = navigator.mediaDevices.enumerateDevices.bind(navigator.mediaDevices);
                    navigator.mediaDevices.enumerateDevices = function() {
                        return origEnumerate().then(function(devices) {
                            return devices.map(function(d) {
                                return {
                                    deviceId: '',
                                    groupId: '',
                                    kind: d.kind,
                                    label: ''
                                };
                            });
                        });
                    };
                }
            })();
        """.trimIndent()
    }
}
