package com.tkno.ren.rentor

import android.app.Application
import android.content.Context
import android.util.Log
import io.matthewnelson.kmp.tor.KmpTorLoaderAndroid
import io.matthewnelson.kmp.tor.manager.TorManager as KmpTorManager
import io.matthewnelson.kmp.tor.manager.common.event.TorManagerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL

/**
 * Ren-Tor Engine: A high-performance, customized Tor daemon controller & privacy engine
 * tailored specifically for Ren Browser.
 */
object RenTorEngine {
    private const val TAG = "RenTorEngine"

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(RenTorEngineState())
    val state: StateFlow<RenTorEngineState> = _state.asStateFlow()

    private val _logs = MutableSharedFlow<RenTorLogEntry>(replay = 50)
    val logs: SharedFlow<RenTorLogEntry> = _logs.asSharedFlow()

    private var kmpTorManager: KmpTorManager? = null
    private var controller: RenTorController? = null
    private var monitorJob: Job? = null
    private var statsJob: Job? = null
    private var isStarting = false

    val bootstrapProgress: Int
        get() = _state.value.bootstrapProgress

    val isConnected: Boolean
        get() = _state.value.status == RenTorStatus.CONNECTED

    /**
     * Checks whether the local SOCKS proxy port is currently listening and accepting connections.
     */
    fun isSocksPortOpen(host: String = "127.0.0.1", port: Int = 9050, timeoutMs: Int = 1200): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun appendLog(level: RenTorLogEntry.LogLevel, message: String) {
        when (level) {
            RenTorLogEntry.LogLevel.INFO -> Log.i(TAG, message)
            RenTorLogEntry.LogLevel.WARN -> Log.w(TAG, message)
            RenTorLogEntry.LogLevel.ERROR -> Log.e(TAG, message)
            RenTorLogEntry.LogLevel.DEBUG -> Log.d(TAG, message)
        }
        engineScope.launch {
            _logs.emit(RenTorLogEntry(level, message))
        }
    }

    @Synchronized
    private fun getOrCreateTorManager(context: Context, settings: RenTorSettings): KmpTorManager {
        kmpTorManager?.let { return it }
        val app = context.applicationContext as Application
        val provider = RenTorConfigProvider(app, settings)
        val loader = KmpTorLoaderAndroid(provider)
        val manager = KmpTorManager.newInstance(app, loader)
        kmpTorManager = manager
        return manager
    }

    /**
     * Starts the Ren-Tor daemon with the given settings.
     */
    fun start(
        context: Context,
        settings: RenTorSettings = RenTorSettings(),
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onComplete: (Boolean, String) -> Unit
    ) {
        val host = "127.0.0.1"
        val port = settings.socksPort
        val ctrlPort = settings.controlPort

        _state.update {
            it.copy(
                socksPort = port,
                controlPort = ctrlPort,
                activeBridge = settings.bridgeType,
                exitCountry = settings.preferredExitCountry
            )
        }

        if (isSocksPortOpen(host, port)) {
            appendLog(RenTorLogEntry.LogLevel.INFO, "Ren-Tor daemon is already running and accessible on port $port.")
            _state.update {
                it.copy(
                    status = RenTorStatus.CONNECTED,
                    bootstrapProgress = 100,
                    bootstrapMessage = "Ren-Tor is active and connected."
                )
            }
            onProgress(100, "Ren-Tor is active and connected.")
            startStatsPolling(ctrlPort)
            onComplete(true, "Connected")
            return
        }

        if (isStarting) {
            onProgress(_state.value.bootstrapProgress, "Ren-Tor is already initializing...")
            return
        }

        isStarting = true
        _state.update {
            it.copy(
                status = RenTorStatus.INITIALIZING,
                bootstrapProgress = 10,
                bootstrapMessage = "Starting Ren-Tor privacy engine...",
                lastError = null
            )
        }
        appendLog(RenTorLogEntry.LogLevel.INFO, "Initializing Ren-Tor daemon on SOCKS:$port CTRL:$ctrlPort")
        onProgress(10, "Starting Ren-Tor privacy engine...")

        val manager = getOrCreateTorManager(context, settings)

        manager.addListener(object : TorManagerEvent.Listener() {
            override fun managerEventState(state: TorManagerEvent.State) {
                appendLog(RenTorLogEntry.LogLevel.DEBUG, "Ren-Tor State: $state")
            }

            override fun managerEventLifecycle(lifecycle: TorManagerEvent.Lifecycle<*>) {
                appendLog(RenTorLogEntry.LogLevel.DEBUG, "Ren-Tor Lifecycle: $lifecycle")
            }

            override fun managerEventInfo(message: String) {
                appendLog(RenTorLogEntry.LogLevel.INFO, message)
                if (message.contains("Bootstrapped")) {
                    val match = Regex("Bootstrapped (\\d+)%").find(message)
                    match?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { percent ->
                        _state.update {
                            it.copy(
                                status = if (percent >= 100) RenTorStatus.CONNECTED else RenTorStatus.BOOTSTRAPPING,
                                bootstrapProgress = percent,
                                bootstrapMessage = message
                            )
                        }
                        engineScope.launch {
                            onProgress(percent, message)
                        }
                    }
                }
            }

            override fun managerEventWarn(message: String) {
                appendLog(RenTorLogEntry.LogLevel.WARN, "Ren-Tor Warning: $message")
            }

            override fun managerEventError(t: Throwable) {
                appendLog(RenTorLogEntry.LogLevel.ERROR, "Ren-Tor Error: ${t.message}")
                _state.update {
                    it.copy(lastError = t.message)
                }
            }
        })

        // Start Tor Daemon
        try {
            manager.startQuietly()
        } catch (e: Exception) {
            appendLog(RenTorLogEntry.LogLevel.ERROR, "Failed to start Tor manager: ${e.message}")
        }

        // Monitor readiness of SOCKS port
        monitorJob?.cancel()
        monitorJob = engineScope.launch(Dispatchers.IO) {
            var attempts = 0
            val maxAttempts = 60 // 60 * 500ms = 30 seconds
            var connected = false

            while (isActive && attempts < maxAttempts) {
                delay(500)
                attempts++
                val open = isSocksPortOpen(host, port)
                if (open) {
                    connected = true
                    _state.update {
                        it.copy(
                            status = RenTorStatus.CONNECTED,
                            bootstrapProgress = 100,
                            bootstrapMessage = "Ren-Tor connection established."
                        )
                    }
                    break
                }
                val progress = (10 + (attempts * 2)).coerceAtMost(95)
                _state.update {
                    it.copy(
                        status = RenTorStatus.BOOTSTRAPPING,
                        bootstrapProgress = progress,
                        bootstrapMessage = "Building Ren-Tor circuits... ($progress%)"
                    )
                }
                withContext(Dispatchers.Main) {
                    onProgress(progress, "Building Ren-Tor circuits... ($progress%)")
                }
            }

            isStarting = false
            withContext(Dispatchers.Main) {
                if (connected) {
                    appendLog(RenTorLogEntry.LogLevel.INFO, "Ren-Tor connection established successfully!")
                    startStatsPolling(ctrlPort)
                    onProgress(100, "Ren-Tor connected successfully!")
                    onComplete(true, "Ren-Tor connected successfully")
                } else {
                    _state.update {
                        it.copy(
                            status = RenTorStatus.ERROR,
                            lastError = "Could not establish Ren-Tor circuit. Please check internet access or bridge settings."
                        )
                    }
                    appendLog(RenTorLogEntry.LogLevel.ERROR, "Ren-Tor bootstrap timeout reached.")
                    onComplete(false, "Could not establish Ren-Tor circuit. Please check internet access or bridge settings.")
                }
            }
        }
    }

    /**
     * Periodically queries live circuits and bandwidth statistics via RenTorController.
     */
    private fun startStatsPolling(controlPort: Int) {
        statsJob?.cancel()
        controller?.close()
        controller = RenTorController(port = controlPort)

        statsJob = engineScope.launch(Dispatchers.IO) {
            controller?.connect()
            while (isActive && isConnected) {
                try {
                    val circuits = controller?.getCircuits() ?: emptyList()
                    val traffic = controller?.getTraffic() ?: RenTorTraffic()
                    _state.update {
                        it.copy(
                            circuits = circuits,
                            traffic = traffic
                        )
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Stats polling tick error: ${e.message}")
                }
                delay(4000)
            }
        }
    }

    /**
     * Request an instantaneous new circuit & identity (SIGNAL NEWNYM).
     * Rotates exit IP and switches Tor relays without restarting the daemon.
     */
    fun renewIdentity(onResult: (Boolean, String) -> Unit) {
        engineScope.launch {
            _state.update { it.copy(status = RenTorStatus.REFRESHING_IDENTITY) }
            val ctrl = controller ?: RenTorController(port = _state.value.controlPort).also { controller = it }
            val success = ctrl.signalNewnym()

            if (success) {
                appendLog(RenTorLogEntry.LogLevel.INFO, "Ren-Tor: New identity requested (Circuit rotated)")
                _state.update {
                    it.copy(
                        status = RenTorStatus.CONNECTED,
                        currentExitIp = null,
                        isTorVerified = false
                    )
                }
                onResult(true, "Ren-Tor identity renewed successfully. New circuits established.")
            } else {
                appendLog(RenTorLogEntry.LogLevel.WARN, "Ren-Tor: Failed to renew identity")
                _state.update { it.copy(status = RenTorStatus.CONNECTED) }
                onResult(false, "Failed to send NEWNYM signal to Ren-Tor daemon.")
            }
        }
    }

    /**
     * Configures preferred Exit Node country codes (e.g. "ch", "is", "de").
     */
    fun setExitCountry(countryCode: String?, strict: Boolean = true, onResult: ((Boolean) -> Unit)? = null) {
        engineScope.launch {
            val ctrl = controller ?: RenTorController(port = _state.value.controlPort).also { controller = it }
            val codes = if (countryCode.isNullOrBlank()) emptyList() else listOf(countryCode.trim().lowercase())
            val ok = ctrl.setExitNodes(codes, strict)
            if (ok) {
                _state.update { it.copy(exitCountry = countryCode) }
                appendLog(RenTorLogEntry.LogLevel.INFO, "Ren-Tor Exit country set to: ${countryCode ?: "Any"}")
            }
            onResult?.invoke(ok)
        }
    }

    /**
     * Stops the Ren-Tor daemon and cancels background monitors.
     */
    fun stop(context: Context) {
        monitorJob?.cancel()
        statsJob?.cancel()
        controller?.close()
        controller = null
        isStarting = false

        _state.update {
            it.copy(
                status = RenTorStatus.DISCONNECTED,
                bootstrapProgress = 0,
                bootstrapMessage = "",
                currentExitIp = null,
                isTorVerified = false,
                circuits = emptyList()
            )
        }

        try {
            kmpTorManager?.stopQuietly()
            appendLog(RenTorLogEntry.LogLevel.INFO, "Ren-Tor daemon stopped.")
        } catch (e: Exception) {
            appendLog(RenTorLogEntry.LogLevel.ERROR, "Error stopping Ren-Tor daemon: ${e.message}")
        }
    }

    /**
     * Verifies routing by querying check.torproject.org through the Ren-Tor SOCKS proxy.
     */
    fun verifyConnection(
        host: String = "127.0.0.1",
        port: Int = 9050,
        onResult: (isTor: Boolean, ip: String?, message: String) -> Unit
    ) {
        engineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port))
                val url = URL("https://check.torproject.org/api/ip")
                conn = url.openConnection(proxy) as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "RenTorBrowser/1.0")

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val isTor = json.optBoolean("IsTor", false)
                    val ip = json.optString("IP", "Unknown")

                    _state.update {
                        it.copy(
                            currentExitIp = ip,
                            isTorVerified = isTor
                        )
                    }

                    withContext(Dispatchers.Main) {
                        onResult(isTor, ip, if (isTor) "Connected via Ren-Tor Exit Node" else "Connected via proxy (Not recognized as Tor exit)")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, null, "HTTP ${conn.responseCode} while checking Ren-Tor status")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, null, "Ren-Tor verification failed: ${e.localizedMessage}")
                }
            } finally {
                conn?.disconnect()
            }
        }
    }
}
