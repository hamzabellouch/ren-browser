package com.tkno.ren.rentor

import android.content.Context
import java.io.File
import java.io.FileOutputStream

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
 * Configuration provider and torrc generator for the Ren-Tor privacy daemon.
 * Self-contained implementation without external dependencies.
 */
class RenTorConfig(
    private val context: Context,
    val settings: RenTorSettings = RenTorSettings()
) {

    val torDir: File by lazy {
        File(context.filesDir, "rentor").apply { mkdirs() }
    }

    val dataDir: File by lazy {
        File(torDir, "data").apply {
            mkdirs()
            try {
                setReadable(true, true)
                setWritable(true, true)
                setExecutable(true, true)
            } catch (_: Exception) {}
        }
    }

    val torrcFile: File by lazy {
        File(torDir, "torrc")
    }

    /**
     * Extracts GeoIP database files from bundled assets if needed.
     */
    fun extractGeoipFiles(): Pair<File?, File?> {
        var geoipFile: File? = null
        var geoip6File: File? = null
        try {
            val am = context.assets
            val g1 = File(torDir, "geoip")
            if (!g1.exists() || g1.length() == 0L) {
                am.open("geoip").use { input ->
                    FileOutputStream(g1).use { output -> input.copyTo(output) }
                }
            }
            if (g1.exists() && g1.length() > 0L) geoipFile = g1

            val g2 = File(torDir, "geoip6")
            if (!g2.exists() || g2.length() == 0L) {
                am.open("geoip6").use { input ->
                    FileOutputStream(g2).use { output -> input.copyTo(output) }
                }
            }
            if (g2.exists() && g2.length() > 0L) geoip6File = g2
        } catch (_: Exception) {}
        return Pair(geoipFile, geoip6File)
    }

    /**
     * Generates a fully compliant torrc file based on [settings].
     */
    fun generateTorrc(): File {
        val lines = mutableListOf<String>()
        lines.add("# Ren-Tor Generated Configuration")
        lines.add("DataDirectory ${dataDir.absolutePath}")

        val (geoip, geoip6) = extractGeoipFiles()
        if (geoip != null) {
            lines.add("GeoIPFile ${geoip.absolutePath}")
        }
        if (geoip6 != null) {
            lines.add("GeoIPv6File ${geoip6.absolutePath}")
        }

        // SOCKS port with circuit isolation
        val isolation = buildString {
            if (settings.isolateDestAddr) append(" IsolateDestAddr")
            if (settings.isolateDestPort) append(" IsolateDestPort")
        }
        lines.add("SocksPort 127.0.0.1:${settings.socksPort}$isolation")

        // Control Port for direct protocol commands (Newnym, circuit query, status)
        lines.add("ControlPort 127.0.0.1:${settings.controlPort}")
        lines.add("CookieAuthentication 0")

        // DNS Port
        lines.add("DNSPort 127.0.0.1:${settings.dnsPort}")

        // Auto-cancel dormant mode when browser initializes
        lines.add("DormantCanceledByStartup 1")

        // Enable Network immediately
        lines.add("DisableNetwork 0")
        lines.add("AvoidDiskWrites 1")

        // Exit node country constraints
        if (!settings.preferredExitCountry.isNullOrBlank()) {
            val cc = settings.preferredExitCountry.trim().lowercase()
            lines.add("ExitNodes {$cc}")
            lines.add("StrictNodes ${if (settings.strictNodes) 1 else 0}")
        }

        // Bridge configuration
        when (settings.bridgeType) {
            BridgeType.NONE -> {
                lines.add("UseBridges 0")
            }
            BridgeType.SNOWFLAKE -> {
                lines.add("UseBridges 1")
                val nativeLibDir = context.applicationInfo.nativeLibraryDir
                val snowflakeClient = File(nativeLibDir, "libsnowflake.so")
                if (snowflakeClient.exists()) {
                    lines.add("ClientTransportPlugin snowflake exec ${snowflakeClient.absolutePath}")
                }
                lines.add("Bridge snowflake 192.0.2.3:1 2B280B23E1107BB62ABFC40DDCC816B6D32973E2")
            }
            BridgeType.OBFS4 -> {
                lines.add("UseBridges 1")
                val nativeLibDir = context.applicationInfo.nativeLibraryDir
                val obfs4Client = File(nativeLibDir, "libobfs4.so")
                if (obfs4Client.exists()) {
                    lines.add("ClientTransportPlugin obfs4 exec ${obfs4Client.absolutePath}")
                }
            }
            BridgeType.CUSTOM -> {
                if (settings.customBridges.isNotEmpty()) {
                    lines.add("UseBridges 1")
                    for (bridge in settings.customBridges) {
                        val clean = bridge.trim()
                        if (clean.isNotEmpty()) {
                            lines.add("Bridge $clean")
                        }
                    }
                } else {
                    lines.add("UseBridges 0")
                }
            }
            else -> {
                lines.add("UseBridges 0")
            }
        }

        torrcFile.writeText(lines.joinToString("\n") + "\n")
        return torrcFile
    }
}
