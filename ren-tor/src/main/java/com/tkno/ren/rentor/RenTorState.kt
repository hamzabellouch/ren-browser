package com.tkno.ren.rentor

/**
 * High-level state representing the Ren-Tor privacy & anonymity engine.
 */
enum class RenTorStatus {
    DISCONNECTED,
    INITIALIZING,
    BOOTSTRAPPING,
    CONNECTED,
    REFRESHING_IDENTITY,
    STOPPING,
    ERROR
}

/**
 * Supported bridge types for censorship evasion in Ren-Tor.
 */
enum class BridgeType(val code: String, val displayName: String) {
    NONE("none", "Direct Connection (No Bridge)"),
    SNOWFLAKE("snowflake", "Snowflake (Anti-Censorship)"),
    OBFS4("obfs4", "obfs4 (Traffic Scrambling)"),
    MEEK_AZURE("meek_azure", "Meek (Domain Fronting)"),
    WEBTUNNEL("webtunnel", "WebTunnel (HTTPS Masking)"),
    CUSTOM("custom", "Custom Bridge Line");

    companion object {
        fun fromCode(code: String): BridgeType =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: NONE
    }
}

/**
 * Represents an active or pending Tor Circuit path.
 */
data class RenTorCircuit(
    val id: String,
    val status: String,
    val path: List<String> = emptyList(),
    val buildTime: Long = System.currentTimeMillis()
)

/**
 * Traffic statistics in bytes and live throughput.
 */
data class RenTorTraffic(
    val bytesRead: Long = 0L,
    val bytesWritten: Long = 0L,
    val readRateBps: Long = 0L,
    val writeRateBps: Long = 0L
)

/**
 * Bootstrap progress and diagnostic step information.
 */
data class RenTorBootstrapInfo(
    val percent: Int = 0,
    val summary: String = "",
    val rawMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Log entry for Ren-Tor diagnostic console.
 */
data class RenTorLogEntry(
    val level: LogLevel = LogLevel.INFO,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class LogLevel { INFO, WARN, ERROR, DEBUG }
}

/**
 * Immutable complete snapshot of the Ren-Tor Engine State.
 */
data class RenTorEngineState(
    val status: RenTorStatus = RenTorStatus.DISCONNECTED,
    val bootstrapProgress: Int = 0,
    val bootstrapMessage: String = "",
    val socksPort: Int = 9050,
    val controlPort: Int = 9051,
    val dnsPort: Int = 9053,
    val currentExitIp: String? = null,
    val isTorVerified: Boolean = false,
    val activeBridge: BridgeType = BridgeType.NONE,
    val exitCountry: String? = null,
    val circuits: List<RenTorCircuit> = emptyList(),
    val traffic: RenTorTraffic = RenTorTraffic(),
    val lastError: String? = null,
    val isStrictZeroLeakActive: Boolean = true
)
