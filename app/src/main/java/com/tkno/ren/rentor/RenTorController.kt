package com.tkno.ren.rentor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Low-level Tor Control Port Client for Ren-Tor Engine.
 * Implements direct socket communication via Tor Control Protocol (RFC-like TC protocol).
 */
class RenTorController(
    private val host: String = "127.0.0.1",
    private val port: Int = 9051,
    private val timeoutMs: Int = 3000
) {
    companion object {
        private const val TAG = "RenTorController"
    }

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStreamWriter? = null
    private var isAuthenticated = false

    /**
     * Connect and authenticate with the Tor Control daemon.
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            close()
            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), timeoutMs)
            sock.soTimeout = timeoutMs

            val r = BufferedReader(InputStreamReader(sock.getInputStream()))
            val w = OutputStreamWriter(sock.getOutputStream())

            socket = sock
            reader = r
            writer = w

            // Authenticate with null password / default cookie
            w.write("AUTHENTICATE \"\"\r\n")
            w.flush()

            val response = r.readLine() ?: ""
            if (response.startsWith("250")) {
                isAuthenticated = true
                Log.d(TAG, "RenTorController authenticated successfully with ControlPort ($port).")
                true
            } else {
                Log.w(TAG, "Tor ControlPort authentication failed: $response")
                isAuthenticated = false
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to connect to RenTor ControlPort at $host:$port: ${e.message}")
            isAuthenticated = false
            close()
            false
        }
    }

    /**
     * Sends a raw command to Tor Control Port and returns the response lines.
     */
    suspend fun sendCommand(command: String): List<String> = withContext(Dispatchers.IO) {
        if (!isAuthenticated || socket == null || socket?.isConnected != true) {
            val ok = connect()
            if (!ok) return@withContext emptyList()
        }

        val responses = mutableListOf<String>()
        try {
            val w = writer ?: return@withContext emptyList()
            val r = reader ?: return@withContext emptyList()

            w.write("$command\r\n")
            w.flush()

            while (true) {
                val line = r.readLine() ?: break
                responses.add(line)
                if (line.startsWith("250 ") || line.startsWith("5") || line.startsWith("4")) {
                    break
                }
                if (line.startsWith("250+")) {
                    // Multi-line response ends with a single '.' line
                    while (true) {
                        val subLine = r.readLine() ?: break
                        responses.add(subLine)
                        if (subLine == "." || subLine.startsWith("250 OK")) {
                            break
                        }
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command '$command' on RenTor ControlPort", e)
            close()
        }
        responses
    }

    /**
     * Signals Tor daemon to build new circuits and switch exit IP (NEWNYM).
     */
    suspend fun signalNewnym(): Boolean = withContext(Dispatchers.IO) {
        val result = sendCommand("SIGNAL NEWNYM")
        val success = result.any { it.startsWith("250 OK") || it.startsWith("250") }
        if (success) {
            Log.d(TAG, "Ren-Tor Identity renewed successfully (SIGNAL NEWNYM delivered).")
        } else {
            Log.w(TAG, "Ren-Tor SIGNAL NEWNYM failed: $result")
        }
        success
    }

    /**
     * Signals Tor daemon to reload configuration (RELOAD / HUP).
     */
    suspend fun signalReload(): Boolean = withContext(Dispatchers.IO) {
        val result = sendCommand("SIGNAL RELOAD")
        result.any { it.startsWith("250") }
    }

    /**
     * Queries active circuits from Tor daemon.
     */
    suspend fun getCircuits(): List<RenTorCircuit> = withContext(Dispatchers.IO) {
        val lines = sendCommand("GETINFO circuit-status")
        val circuits = mutableListOf<RenTorCircuit>()

        for (line in lines) {
            if (line.startsWith("250") || line == ".") continue
            // Format: 1 BUILT $FINGERPRINT~Name,$FINGERPRINT~Name PURPOSE=GENERAL ...
            val parts = line.split(" ")
            if (parts.size >= 2) {
                val id = parts[0]
                val status = parts[1]
                val path = if (parts.size > 2 && !parts[2].startsWith("PURPOSE")) {
                    parts[2].split(",")
                } else {
                    emptyList()
                }
                circuits.add(RenTorCircuit(id = id, status = status, path = path))
            }
        }
        circuits
    }

    /**
     * Queries live bandwidth consumption from Tor daemon.
     */
    suspend fun getTraffic(): RenTorTraffic = withContext(Dispatchers.IO) {
        val lines = sendCommand("GETINFO traffic/read traffic/written")
        var bytesRead = 0L
        var bytesWritten = 0L

        for (line in lines) {
            if (line.startsWith("250-traffic/read=")) {
                bytesRead = line.substringAfter("=").trim().toLongOrNull() ?: 0L
            } else if (line.startsWith("250-traffic/written=") || line.startsWith("250 traffic/written=")) {
                bytesWritten = line.substringAfter("=").trim().toLongOrNull() ?: 0L
            }
        }
        RenTorTraffic(bytesRead = bytesRead, bytesWritten = bytesWritten)
    }

    /**
     * Configures preferred Exit Node Countries (e.g., "{ch},{de},{is}").
     */
    suspend fun setExitNodes(countryCodes: List<String>, strict: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        if (countryCodes.isEmpty()) {
            val res1 = sendCommand("RESETCONF ExitNodes")
            val res2 = sendCommand("RESETCONF StrictNodes")
            res1.any { it.startsWith("250") } && res2.any { it.startsWith("250") }
        } else {
            val nodes = countryCodes.joinToString(",") { "{$it}" }
            val res1 = sendCommand("SETCONF ExitNodes=$nodes")
            val res2 = sendCommand("SETCONF StrictNodes=${if (strict) "1" else "0"}")
            res1.any { it.startsWith("250") } && res2.any { it.startsWith("250") }
        }
    }

    /**
     * Safely closes the control socket.
     */
    fun close() {
        try {
            reader?.close()
        } catch (_: Exception) {}
        try {
            writer?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        reader = null
        writer = null
        socket = null
        isAuthenticated = false
    }
}
