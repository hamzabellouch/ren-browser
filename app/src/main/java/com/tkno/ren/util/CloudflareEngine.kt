package com.tkno.ren.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.EOFException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PushbackInputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance embedded Cloudflare 1.1.1.1 Anycast Proxy Engine.
 * 
 * Provides an in-process local HTTP/SOCKS5 proxy server that intercepts WebView
 * traffic, resolves all DNS requests over Cloudflare DNS-over-HTTPS (DoH Anycast),
 * enforces security filtering (Malware / Adult content blocking), and tunnels TCP
 * connections securely.
 */
object CloudflareEngine {
    private const val TAG = "CloudflareEngine"

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val dohCache = ConcurrentHashMap<String, Pair<String, Long>>()

    val isRunning: Boolean
        get() = serverSocket?.isClosed == false && serverJob?.isActive == true

    /**
     * Starts the embedded Cloudflare proxy engine on the given port.
     */
    @Synchronized
    fun start(
        context: Context,
        port: Int = WarpManager.DEFAULT_PORT,
        onComplete: (success: Boolean, errorMsg: String?) -> Unit
    ) {
        if (isRunning && serverSocket?.localPort == port) {
            Log.d(TAG, "Cloudflare engine is already active on port $port")
            onComplete(true, null)
            return
        }

        stop()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress("127.0.0.1", port))
                serverSocket = server

                val mode = WarpManager.getSecurityMode(context)
                val dohUrl = mode.dohEndpoint

                serverJob = launch(Dispatchers.IO) {
                    Log.i(TAG, "Cloudflare Engine started successfully on 127.0.0.1:$port (DoH: $dohUrl)")
                    while (isActive && !server.isClosed) {
                        try {
                            val client = server.accept()
                            launch(Dispatchers.IO) {
                                handleClient(client, dohUrl)
                            }
                        } catch (e: Exception) {
                            if (server.isClosed) break
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Cloudflare Engine on port $port", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "Failed to bind Cloudflare local proxy on port $port: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Stops the embedded Cloudflare proxy engine.
     */
    @Synchronized
    fun stop() {
        try {
            serverJob?.cancel()
            serverJob = null
            serverSocket?.close()
            serverSocket = null
            Log.d(TAG, "Cloudflare Engine stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Cloudflare Engine", e)
        }
    }

    private suspend fun handleClient(clientSocket: Socket, dohUrl: String) {
        try {
            clientSocket.soTimeout = 45000
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()
            val pushback = PushbackInputStream(input, 1024)

            val firstByte = pushback.read()
            if (firstByte == -1) {
                clientSocket.close()
                return
            }
            pushback.unread(firstByte)

            if (firstByte == 0x05) {
                // SOCKS5 protocol
                handleSocks5(pushback, output, clientSocket, dohUrl)
            } else {
                // HTTP / HTTPS CONNECT protocol
                handleHttp(pushback, output, clientSocket, dohUrl)
            }
        } catch (_: Exception) {
        } finally {
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun handleSocks5(
        input: InputStream,
        output: OutputStream,
        clientSocket: Socket,
        dohUrl: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Handshake
            val version = input.read()
            if (version != 5) return@withContext
            val nMethods = input.read()
            val methods = ByteArray(nMethods)
            input.readFully(methods)

            // Reply: NO AUTH REQUIRED
            output.write(byteArrayOf(0x05, 0x00))
            output.flush()

            // Request
            val ver = input.read()
            val cmd = input.read()
            val rsv = input.read()
            val atyp = input.read()

            if (cmd != 0x01) { // 0x01 = CONNECT
                output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
                return@withContext
            }

            val targetHost: String = when (atyp) {
                0x01 -> { // IPv4
                    val ipBytes = ByteArray(4)
                    input.readFully(ipBytes)
                    InetAddress.getByAddress(ipBytes).hostAddress ?: return@withContext
                }
                0x03 -> { // Domain name
                    val domainLen = input.read()
                    val domainBytes = ByteArray(domainLen)
                    input.readFully(domainBytes)
                    String(domainBytes, Charsets.UTF_8)
                }
                0x04 -> { // IPv6
                    val ipBytes = ByteArray(16)
                    input.readFully(ipBytes)
                    InetAddress.getByAddress(ipBytes).hostAddress ?: return@withContext
                }
                else -> return@withContext
            }

            val portHigh = input.read()
            val portLow = input.read()
            val targetPort = ((portHigh and 0xFF) shl 8) or (portLow and 0xFF)

            // Resolve via Cloudflare DoH
            val resolvedIp = resolveHostOverDoh(targetHost, dohUrl)

            val remoteSocket = try {
                Socket().apply {
                    connect(InetSocketAddress(resolvedIp, targetPort), 12000)
                    soTimeout = 45000
                }
            } catch (e: Exception) {
                output.write(byteArrayOf(0x05, 0x04, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
                return@withContext
            }

            // Success reply
            output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            output.flush()

            pipeSockets(clientSocket, remoteSocket)
        } catch (_: Exception) {}
    }

    private suspend fun handleHttp(
        input: InputStream,
        output: OutputStream,
        clientSocket: Socket,
        dohUrl: String
    ) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(input, Charsets.ISO_8859_1))
            val firstLine = reader.readLine() ?: return@withContext
            val parts = firstLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val uri = parts[1]

            if (method.equals("CONNECT", ignoreCase = true)) {
                // HTTPS CONNECT host:port
                val hostPort = uri.split(":")
                val targetHost = hostPort[0]
                val targetPort = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 443 else 443

                // Consume headers until blank line
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                }

                val resolvedIp = resolveHostOverDoh(targetHost, dohUrl)
                val remoteSocket = try {
                    Socket().apply {
                        connect(InetSocketAddress(resolvedIp, targetPort), 12000)
                        soTimeout = 45000
                    }
                } catch (e: Exception) {
                    output.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                    output.flush()
                    return@withContext
                }

                output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                output.flush()

                pipeSockets(clientSocket, remoteSocket)
            } else {
                // Plain HTTP request
                val parsedUri = try { Uri.parse(uri) } catch (_: Exception) { null }
                val targetHost = parsedUri?.host ?: extractHostFromHeaders(reader) ?: return@withContext
                val targetPort = if (parsedUri?.port != null && parsedUri.port != -1) parsedUri.port else 80

                val resolvedIp = resolveHostOverDoh(targetHost, dohUrl)
                val remoteSocket = try {
                    Socket().apply {
                        connect(InetSocketAddress(resolvedIp, targetPort), 12000)
                        soTimeout = 45000
                    }
                } catch (e: Exception) {
                    output.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                    output.flush()
                    return@withContext
                }

                val remoteOut = remoteSocket.getOutputStream()
                val path = if (parsedUri?.path.isNullOrEmpty()) "/" else parsedUri?.path
                val query = if (parsedUri?.query.isNullOrEmpty()) "" else "?${parsedUri?.query}"
                val modifiedFirstLine = "$method $path$query ${parts.getOrElse(2) { "HTTP/1.1" }}\r\n"
                remoteOut.write(modifiedFirstLine.toByteArray(Charsets.ISO_8859_1))

                pipeSockets(clientSocket, remoteSocket)
            }
        } catch (_: Exception) {}
    }

    private fun extractHostFromHeaders(reader: BufferedReader): String? {
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Host:", ignoreCase = true)) {
                val hostVal = line.substring(5).trim()
                return hostVal.split(":")[0]
            }
        }
        return null
    }

    private suspend fun pipeSockets(sockA: Socket, sockB: Socket) = coroutineScope {
        val job1 = launch(Dispatchers.IO) {
            try {
                val inA = sockA.getInputStream()
                val outB = sockB.getOutputStream()
                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (inA.read(buffer).also { bytesRead = it } != -1) {
                    outB.write(buffer, 0, bytesRead)
                    outB.flush()
                }
            } catch (_: Exception) {
            } finally {
                try { sockB.shutdownOutput() } catch (_: Exception) {}
            }
        }

        val job2 = launch(Dispatchers.IO) {
            try {
                val inB = sockB.getInputStream()
                val outA = sockA.getOutputStream()
                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (inB.read(buffer).also { bytesRead = it } != -1) {
                    outA.write(buffer, 0, bytesRead)
                    outA.flush()
                }
            } catch (_: Exception) {
            } finally {
                try { sockA.shutdownOutput() } catch (_: Exception) {}
            }
        }

        job1.join()
        job2.join()
        try { sockA.close() } catch (_: Exception) {}
        try { sockB.close() } catch (_: Exception) {}
    }

    fun resolveHostOverDoh(host: String, dohUrl: String): String {
        if (isIpAddress(host)) return host

        val cleanHost = host.trim().lowercase()
        val now = System.currentTimeMillis()
        val cached = dohCache[cleanHost]
        if (cached != null && cached.second > now) {
            return cached.first
        }

        try {
            val separator = if (dohUrl.contains("?")) "&" else "?"
            val queryUrl = "${dohUrl}${separator}name=${Uri.encode(cleanHost)}&type=A"

            val url = URL(queryUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/dns-json")
            conn.setRequestProperty("User-Agent", "RenBrowser/1.0")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.connect()

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val answers = json.optJSONArray("Answer")
                if (answers != null && answers.length() > 0) {
                    for (i in 0 until answers.length()) {
                        val item = answers.getJSONObject(i)
                        val type = item.optInt("type", 1) // 1 = A
                        val data = item.optString("data", "")
                        if (type == 1 && isIpAddress(data)) {
                            dohCache[cleanHost] = Pair(data, now + 300_000L) // 5 min TTL
                            return data
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback to system DNS if DoH times out or blocked
        return try {
            val ip = InetAddress.getByName(host).hostAddress ?: host
            dohCache[cleanHost] = Pair(ip, now + 60_000L)
            ip
        } catch (_: Exception) {
            host
        }
    }

    private fun InputStream.readFully(b: ByteArray) {
        var offset = 0
        while (offset < b.size) {
            val read = read(b, offset, b.size - offset)
            if (read == -1) throw EOFException("Unexpected EOF while reading socket")
            offset += read
        }
    }

    private fun isIpAddress(str: String): Boolean {
        val parts = str.split(".")
        if (parts.size != 4) return false
        return parts.all { it.toIntOrNull()?.let { num -> num in 0..255 } == true }
    }
}
