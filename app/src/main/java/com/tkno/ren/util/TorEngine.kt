package com.tkno.ren.util

import android.app.Application
import android.content.Context
import android.util.Log
import io.matthewnelson.kmp.tor.KmpTorLoaderAndroid
import io.matthewnelson.kmp.tor.TorConfigProviderAndroid
import io.matthewnelson.kmp.tor.common.address.PortProxy
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig
import io.matthewnelson.kmp.tor.manager.TorManager as KmpTorManager
import io.matthewnelson.kmp.tor.manager.common.event.TorManagerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL

class RenTorConfigProvider(
    context: Context,
    private val socksPort: Int = 9050,
    private val controlPort: Int = 9051
) : TorConfigProviderAndroid(context) {
    override fun provide(): TorConfig {
        return TorConfig.Builder().apply {
            put(
                TorConfig.Setting.Ports.Socks().set(
                    TorConfig.Option.AorDorPort.Value(PortProxy(socksPort))
                )
            )
            put(
                TorConfig.Setting.Ports.Control().set(
                    TorConfig.Option.AorDorPort.Value(PortProxy(controlPort))
                )
            )
            put(
                TorConfig.Setting.DormantCanceledByStartup().set(TorConfig.Option.TorF.True)
            )
            put(
                TorConfig.Setting.DisableNetwork().set(TorConfig.Option.TorF.False)
            )
        }.build()
    }
}

object TorEngine {
    private const val TAG = "TorEngine"

    private var kmpTorManager: KmpTorManager? = null
    private var monitorJob: Job? = null
    private var isStarting = false

    var bootstrapProgress: Int = 0
        private set

    fun isSocksPortOpen(host: String = "127.0.0.1", port: Int = 9050, timeoutMs: Int = 1500): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun getOrCreateTorManager(context: Context, socksPort: Int = 9050, controlPort: Int = 9051): KmpTorManager {
        kmpTorManager?.let { return it }
        val app = context.applicationContext as Application
        val provider = RenTorConfigProvider(app, socksPort, controlPort)
        val loader = KmpTorLoaderAndroid(provider)
        val manager = KmpTorManager.newInstance(app, loader)
        kmpTorManager = manager
        return manager
    }

    fun startTor(
        context: Context,
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onComplete: (Boolean, String) -> Unit
    ) {
        val host = TorManager.getProxyHost(context)
        val port = TorManager.getProxyPort(context)

        if (isSocksPortOpen(host, port)) {
            Log.d(TAG, "Tor daemon is already running and accessible on port $port.")
            bootstrapProgress = 100
            onProgress(100, "Tor is active and connected.")
            onComplete(true, "Connected")
            return
        }

        if (isStarting) {
            onProgress(bootstrapProgress, "Tor is already initializing...")
            return
        }

        isStarting = true
        bootstrapProgress = 10
        onProgress(10, "Starting Official Tor Daemon...")

        val manager = getOrCreateTorManager(context, socksPort = port)

        manager.addListener(object : TorManagerEvent.Listener() {
            override fun managerEventState(state: TorManagerEvent.State) {
                Log.d(TAG, "Tor state: $state")
            }

            override fun managerEventLifecycle(event: TorManagerEvent.Lifecycle<*>) {
                Log.d(TAG, "Tor lifecycle: $event")
            }

            override fun managerEventInfo(message: String) {
                Log.d(TAG, "Tor info: $message")
                if (message.contains("Bootstrapped")) {
                    val match = Regex("Bootstrapped (\\d+)%").find(message)
                    match?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { percent ->
                        bootstrapProgress = percent
                        CoroutineScope(Dispatchers.Main).launch {
                            onProgress(percent, message)
                        }
                    }
                }
            }

            override fun managerEventWarn(message: String) {
                Log.w(TAG, "Tor warn: $message")
            }

            override fun managerEventError(throwable: Throwable) {
                Log.e(TAG, "Tor error", throwable)
            }
        })

        // Start Tor
        manager.startQuietly()

        // Monitor readiness of SOCKS port
        monitorJob?.cancel()
        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            var attempts = 0
            val maxAttempts = 40 // 40 * 500ms = 20s
            var connected = false

            while (isActive && attempts < maxAttempts) {
                delay(500)
                attempts++
                val open = isSocksPortOpen(host, port)
                if (open) {
                    connected = true
                    bootstrapProgress = 100
                    break
                }
                val progress = (10 + (attempts * 2)).coerceAtMost(95)
                bootstrapProgress = progress
                withContext(Dispatchers.Main) {
                    onProgress(progress, "Building Tor circuits... ($progress%)")
                }
            }

            isStarting = false
            withContext(Dispatchers.Main) {
                if (connected) {
                    onProgress(100, "Tor connected successfully!")
                    onComplete(true, "Tor connected successfully")
                } else {
                    onComplete(false, "Could not establish Tor circuit. Please check network.")
                }
            }
        }
    }

    fun stopTor(context: Context) {
        monitorJob?.cancel()
        isStarting = false
        bootstrapProgress = 0
        try {
            kmpTorManager?.stopQuietly()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TorManager", e)
        }
    }

    fun verifyTorConnection(
        host: String = "127.0.0.1",
        port: Int = 9050,
        onResult: (isTor: Boolean, ip: String?, message: String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var conn: HttpURLConnection? = null
            try {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port))
                val url = URL("https://check.torproject.org/api/ip")
                conn = url.openConnection(proxy) as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "RenBrowser/1.0")

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val isTor = json.optBoolean("IsTor", false)
                    val ip = json.optString("IP", "Unknown")
                    withContext(Dispatchers.Main) {
                        onResult(isTor, ip, if (isTor) "Connected via Tor Exit Node" else "Connected but not recognized as Tor")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, null, "HTTP ${conn.responseCode} while checking Tor status")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, null, "Verification failed: ${e.localizedMessage}")
                }
            } finally {
                conn?.disconnect()
            }
        }
    }
}
