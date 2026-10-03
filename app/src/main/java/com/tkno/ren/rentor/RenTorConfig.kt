package com.tkno.ren.rentor

import android.content.Context
import io.matthewnelson.kmp.tor.TorConfigProviderAndroid
import io.matthewnelson.kmp.tor.common.address.PortProxy
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig

/**
 * Configuration definition for the Ren-Tor Engine.
 */
data class RenTorSettings(
    val socksPort: Int = 9050,
    val controlPort: Int = 9051,
    val dnsPort: Int = 9053,
    val bridgeType: BridgeType = BridgeType.NONE,
    val customBridges: List<String> = emptyList(),
    val preferredExitCountry: String? = null,
    val strictNodes: Boolean = false,
    val isolateDestPort: Boolean = true,
    val isolateDestAddr: Boolean = true,
    val blockGeolocation: Boolean = true
)

/**
 * Android Provider for compiling and supplying configuration to the underlying Tor daemon.
 */
class RenTorConfigProvider(
    context: Context,
    private val settings: RenTorSettings
) : TorConfigProviderAndroid(context) {

    override fun provide(): TorConfig {
        return TorConfig.Builder().apply {
            // SOCKS Proxy Port
            put(
                TorConfig.Setting.Ports.Socks().set(
                    TorConfig.Option.AorDorPort.Value(PortProxy(settings.socksPort))
                )
            )

            // Control Port for direct protocol commands (Newnym, circuit query, status)
            put(
                TorConfig.Setting.Ports.Control().set(
                    TorConfig.Option.AorDorPort.Value(PortProxy(settings.controlPort))
                )
            )

            // Auto-cancel dormant mode when browser initializes
            put(
                TorConfig.Setting.DormantCanceledByStartup().set(TorConfig.Option.TorF.True)
            )

            // Enable Network immediately
            put(
                TorConfig.Setting.DisableNetwork().set(TorConfig.Option.TorF.False)
            )
        }.build()
    }
}
