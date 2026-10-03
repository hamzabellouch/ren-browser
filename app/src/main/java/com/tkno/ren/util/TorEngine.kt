package com.tkno.ren.util

import android.content.Context
import com.tkno.ren.rentor.RenTorEngine
import com.tkno.ren.rentor.RenTorManager

/**
 * Legacy compatibility alias for [RenTorEngine].
 */
object TorEngine {
    val bootstrapProgress: Int
        get() = RenTorEngine.bootstrapProgress

    fun isSocksPortOpen(host: String = "127.0.0.1", port: Int = 9050, timeoutMs: Int = 1500): Boolean {
        return RenTorEngine.isSocksPortOpen(host, port, timeoutMs)
    }

    fun startTor(
        context: Context,
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onComplete: (Boolean, String) -> Unit
    ) {
        val settings = RenTorManager.getSettings(context)
        RenTorEngine.start(
            context = context,
            settings = settings,
            onProgress = onProgress,
            onComplete = onComplete
        )
    }

    fun stopTor(context: Context) {
        RenTorEngine.stop(context)
    }

    fun renewIdentity(onResult: (Boolean, String) -> Unit) {
        RenTorEngine.renewIdentity(onResult)
    }

    fun verifyTorConnection(
        host: String = "127.0.0.1",
        port: Int = 9050,
        onResult: (isTor: Boolean, ip: String?, message: String) -> Unit
    ) {
        RenTorEngine.verifyConnection(host, port, onResult)
    }
}
