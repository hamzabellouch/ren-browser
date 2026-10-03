package com.tkno.ren.util

import android.content.Context
import com.tkno.ren.rentor.BridgeType
import com.tkno.ren.rentor.RenTorEngine
import com.tkno.ren.rentor.RenTorManager
import com.tkno.ren.rentor.RenTorStatus

enum class TorStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Legacy compatibility delegate for [RenTorManager].
 */
object TorManager {
    val status: TorStatus
        get() = when (RenTorEngine.state.value.status) {
            RenTorStatus.DISCONNECTED, RenTorStatus.STOPPING -> TorStatus.DISCONNECTED
            RenTorStatus.INITIALIZING, RenTorStatus.BOOTSTRAPPING, RenTorStatus.REFRESHING_IDENTITY -> TorStatus.CONNECTING
            RenTorStatus.CONNECTED -> TorStatus.CONNECTED
            RenTorStatus.ERROR -> TorStatus.ERROR
        }

    fun isTorEnabled(context: Context): Boolean = RenTorManager.isTorEnabled(context)

    fun setTorEnabled(context: Context, enabled: Boolean) = RenTorManager.setTorEnabled(context, enabled)

    fun isUseBridgesEnabled(context: Context): Boolean = RenTorManager.isUseBridgesEnabled(context)

    fun setUseBridgesEnabled(context: Context, enabled: Boolean) = RenTorManager.setUseBridgesEnabled(context, enabled)

    fun getBridgeType(context: Context): String = RenTorManager.getBridgeType(context).code

    fun setBridgeType(context: Context, type: String) = RenTorManager.setBridgeType(context, BridgeType.fromCode(type))

    fun getCustomBridges(context: Context): String = RenTorManager.getCustomBridges(context)

    fun setCustomBridges(context: Context, bridges: String) = RenTorManager.setCustomBridges(context, bridges)

    fun getProxyHost(context: Context): String = RenTorManager.getProxyHost(context)

    fun setProxyHost(context: Context, host: String) = RenTorManager.setProxyHost(context, host)

    fun getProxyPort(context: Context): Int = RenTorManager.getProxyPort(context)

    fun setProxyPort(context: Context, port: Int) = RenTorManager.setProxyPort(context, port)

    fun isBlockGeolocation(context: Context): Boolean = RenTorManager.isBlockGeolocation(context)

    fun setBlockGeolocation(context: Context, enabled: Boolean) = RenTorManager.setBlockGeolocation(context, enabled)

    fun applyProxyOverride(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        RenTorManager.applyProxyOverride(context, onComplete)
    }

    fun clearProxyOverride(context: Context? = null, onComplete: ((Boolean) -> Unit)? = null) {
        RenTorManager.clearProxyOverride(context, onComplete)
    }

    fun toggleTor(
        context: Context,
        onProgress: ((Int, String) -> Unit)? = null,
        onComplete: ((Boolean, Boolean) -> Unit)? = null
    ) {
        RenTorManager.toggleTor(context, onProgress, onComplete)
    }

    fun renewIdentity(onResult: (Boolean, String) -> Unit) {
        RenTorManager.renewIdentity(onResult)
    }

    fun initAtStartup(context: Context) {
        RenTorManager.initAtStartup(context)
    }
}
