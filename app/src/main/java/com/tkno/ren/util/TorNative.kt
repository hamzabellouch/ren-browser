package com.tkno.ren.util

import android.util.Log

object TorNative {
    private const val TAG = "TorNative"
    var isLoaded: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("tor")
            isLoaded = true
            Log.d(TAG, "Native Tor library loaded successfully via JNI.")
        } catch (e: UnsatisfiedLinkError) {
            isLoaded = false
            Log.w(TAG, "Native Tor library not loaded via System.loadLibrary: ${e.message}")
        } catch (e: Throwable) {
            isLoaded = false
            Log.w(TAG, "Unexpected error loading native Tor library: ${e.message}")
        }
    }

    external fun torMain(args: Array<String>): Int
    external fun torStop(): Int
}
