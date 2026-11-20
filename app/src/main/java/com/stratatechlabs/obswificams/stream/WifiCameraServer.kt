package com.stratatechlabs.obswificams.stream

import android.util.Log
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Embedded HTTP server streaming Motion JPEG (MJPEG) frames and live WAV audio to browsers,
 * OBS Studio, and VLC over local Wi-Fi. Also provides REST endpoints for full remote camera control.
 */
class WifiCameraServer(
    val port: Int,
    private val hostAddress: String,
    private val callbacks: ServerCallbacks,
    private val audioStreamer: WifiAudioStreamer? = null,
) {

    interface ServerCallbacks {
        fun getLatestSnapshot(): ByteArray?
        fun openLatestVideoStream(): Pair<InputStream, Long>? = null
        fun onSwitchCamera(): Boolean
        fun onSelectLens(lensId: String): Boolean = false
        fun onSelectMode(mode: String): Boolean = false
        fun onToggleTorch(): Boolean
        fun onSetTorchStrength(level: Int): Boolean = false
        fun onTriggerCapture(): Boolean
        fun onToggleRecording(): Boolean = false
        fun onSetZoom(ratio: Float): Boolean = false
        fun onSetExposure(index: Int): Boolean = false
        fun onSetQuality(qualityKey: String): Boolean = false
        fun onSetFps(fps: String): Boolean = false
        fun onSetAspectRatio(aspect: String): Boolean = false
        fun onSetFlash(mode: String): Boolean = false
        fun onSetProIso(iso: Int?): Boolean = false
        fun onSetProAwb(awb: Int?): Boolean = false
        fun onSetProFocusDiopters(diopters: Float?): Boolean = false
        fun onSetAutoAdjust(enabled: Boolean): Boolean = false
        fun onSetRotation(degrees: Int): Boolean = false
        fun onSetFilter(name: String): Boolean = false
        fun onRewardTeaReceived(): Boolean = false
        fun onClientCountChanged(count: Int)
        fun getStatusInfo(): StatusInfo
    }

    data class LensInfo(
        val id: String,
        val name: String,
        val isFront: Boolean,
    )

    data class StatusInfo(
        val isStreaming: Boolean = true,
        val clientCount: Int = 0,
        val fps: Int = 0,
        val targetFps: String = "auto",
        val resolution: String = "1280x720",
        val facing: String = "back",
        val isTorchOn: Boolean = false,
        val torchStrength: Int = 1,
        val torchStrengthMax: Int = 1,
        val minZoom: Float = 1.0f,
        val maxZoom: Float = 1.0f,
        val zoomRatio: Float = 1.0f,
        val evMin: Int = 0,
        val evMax: Int = 0,
        val evIndex: Int = 0,
        val isRecording: Boolean = false,
        val recordingSeconds: Int = 0,
        val audioSupported: Boolean = false,
        val audioLevel: Int = 0,
        val quality: String = "720p",
        val aspect: String = "16:9",
        val autoAdjust: Boolean = true,
        val rotation: Int = 0,
        val iso: Int? = null,
        val awb: Int = 1,
        val focus: Float? = null,
        val mode: String = "photo",
        val flash: String = "off",
        val filter: String = "none",
        val lenses: List<LensInfo> = emptyList(),
    )

    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null

    private val connectionExecutor: ExecutorService = Executors.newCachedThreadPool()
    private val frameBroadcastExecutor: ExecutorService = Executors.newFixedThreadPool(4)
    private val activeStreamClients = Collections.newSetFromMap(ConcurrentHashMap<StreamClient, Boolean>())

    val activeClientCount: Int
        get() = activeStreamClients.size + (audioStreamer?.clientCount ?: 0)

    val deliveredFramesCount = AtomicInteger(0)
    val droppedFramesCount = AtomicInteger(0)

    private val crlfBytes = "\r\n".toByteArray(StandardCharsets.US_ASCII)

    fun getAndResetDropStats(): Pair<Int, Int> =
        Pair(deliveredFramesCount.getAndSet(0), droppedFramesCount.getAndSet(0))

    fun hasClients(): Boolean = activeStreamClients.isNotEmpty()

    fun start() {
        if (isRunning.getAndSet(true)) return

        val s = ServerSocket(port).apply {
            reuseAddress = true
            setPerformancePreferences(0, 2, 1)
        }
        serverSocket = s

        acceptThread = Thread(
            {
                while (isRunning.get()) {
                    try {
                        val socket = s.accept()
                        socket.tcpNoDelay = true
                        socket.trafficClass = 0x10 // IPTOS_LOWDELAY
                        connectionExecutor.execute { handleConnection(socket) }
                    } catch (e: Exception) {
                        if (!isRunning.get()) break
                        Log.w(TAG, "Accept error", e)
                    }
                }
            },
            "WifiCameraServer-Accept",
        ).also { it.start() }

        Log.i(TAG, "Wifi camera server started on port $port")
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return

        runCatching { serverSocket?.close() }
        serverSocket = null

        acceptThread?.interrupt()
        acceptThread = null

        activeStreamClients.forEach { it.close() }
        activeStreamClients.clear()
        callbacks.onClientCountChanged(0)

        connectionExecutor.shutdownNow()
        frameBroadcastExecutor.shutdownNow()
        Log.i(TAG, "Wifi camera server stopped")
    }

    fun broadcastFrame(jpegBytes: ByteArray) {
        if (!isRunning.get() || activeStreamClients.isEmpty()) return

        // Pre-build boundary header once for all clients to eliminate per-client allocation overhead
        val headerBytes = ("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: " +
            jpegBytes.size + "\r\n\r\n").toByteArray(StandardCharsets.US_ASCII)

        for (client in activeStreamClients) {
            if (client.isClosed.get()) {
                activeStreamClients.remove(client)
                notifyClientCount()
                continue
            }

            // Non-blocking write: if client socket is still transmitting previous frame,
            // drop this frame for this specific client to preserve ultra-low latency.
            if (client.isWriting.compareAndSet(false, true)) {
                frameBroadcastExecutor.execute {
                    try {
                        client.output.write(headerBytes)
                        client.output.write(jpegBytes)
                        client.output.write(crlfBytes)
                        client.output.flush()
                        deliveredFramesCount.incrementAndGet()
                    } catch (_: IOException) {
                        client.close()
                        activeStreamClients.remove(client)
                        notifyClientCount()
                    } finally {
                        client.isWriting.set(false)
                    }
                }
            } else {
                droppedFramesCount.incrementAndGet()
            }
        }
    }

    private fun notifyClientCount() {
        callbacks.onClientCountChanged(activeClientCount)
    }

    private fun handleConnection(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
            val requestLine = reader.readLine() ?: run {
                socket.close()
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return
            }

            val method = parts[0].uppercase()
            val fullPath = parts[1]
            val path = fullPath.substringBefore('?')
            val queryString = fullPath.substringAfter('?', "")
            val queryParams = parseQueryParams(queryString)

            // Read headers until empty line
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }

            if (method == "OPTIONS") {
                sendOptionsResponse(socket)
                return
            }

            when (path) {
                "/", "/index.html" -> serveIndex(socket)
                "/obs" -> serveObs(socket)
                "/video.mjpeg", "/video", "/stream.mjpeg", "/stream" -> serveMjpegStream(socket)
                "/audio.wav", "/audio", "/stream.wav" -> {
                    if (audioStreamer != null) {
                        audioStreamer.handleAudioClient(socket)
                        notifyClientCount()
                    } else {
                        send503(socket)
                    }
                }
                "/snapshot.jpg", "/photo.jpg" -> serveSnapshot(socket)
                "/latest-video.mp4", "/video/latest" -> serveLatestVideo(socket)
                "/status" -> serveStatus(socket)
                "/api/switch-camera" -> {
                    val success = callbacks.onSwitchCamera()
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/select-camera" -> {
                    val id = queryParams["id"] ?: ""
                    val success = callbacks.onSelectLens(id)
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/mode" -> {
                    val mode = queryParams["mode"] ?: "photo"
                    val success = callbacks.onSelectMode(mode)
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/torch" -> {
                    val state = callbacks.onToggleTorch()
                    sendJsonResponse(socket, """{"torch":$state,"isTorchOn":$state}""")
                }
                "/api/torch-strength" -> {
                    val level = queryParams["level"]?.toIntOrNull() ?: 1
                    val success = callbacks.onSetTorchStrength(level)
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/flash" -> {
                    val option = queryParams["option"] ?: "off"
                    val success = callbacks.onSetFlash(option)
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/photo", "/api/capture" -> {
                    val success = callbacks.onTriggerCapture()
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/record" -> {
                    val state = callbacks.onToggleRecording()
                    sendJsonResponse(socket, """{"recording":$state,"isRecording":$state}""")
                }
                "/api/zoom" -> {
                    val ratio = queryParams["ratio"]?.toFloatOrNull() ?: 1f
                    val success = callbacks.onSetZoom(ratio)
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/exposure" -> {
                    val index = queryParams["index"]?.toIntOrNull() ?: 0
                    val success = callbacks.onSetExposure(index)
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/quality" -> {
                    val q = queryParams["quality"] ?: "720p"
                    val success = callbacks.onSetQuality(q)
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/fps" -> {
                    val fps = queryParams["fps"] ?: "auto"
                    val success = callbacks.onSetFps(fps)
                    sendJsonResponse(socket, """{"success":$success,"fps":"$fps"}""")
                }
                "/api/aspect" -> {
                    val aspect = queryParams["aspect"] ?: "16:9"
                    val success = callbacks.onSetAspectRatio(aspect)
                    sendJsonResponse(socket, """{"success":$success}""")
                }
                "/api/pro-manual" -> {
                    val rawIso = queryParams["iso"]
                    val rawAwb = queryParams["awb"]
                    val rawFocus = queryParams["focusDiopters"]

                    if (rawIso != null) {
                        val iso = if (rawIso.equals("auto", ignoreCase = true) || (rawIso.toIntOrNull() == -1)) null else rawIso.toIntOrNull()
                        callbacks.onSetProIso(iso)
                    }
                    if (rawAwb != null) {
                        val awb = if (rawAwb.equals("auto", ignoreCase = true) || (rawAwb.toIntOrNull() == -1) || (rawAwb.toIntOrNull() == 1)) null else rawAwb.toIntOrNull()
                        callbacks.onSetProAwb(awb)
                    }
                    if (rawFocus != null) {
                        val focus = if (rawFocus.equals("auto", ignoreCase = true) || (rawFocus.toFloatOrNull() == -1f)) null else rawFocus.toFloatOrNull()
                        callbacks.onSetProFocusDiopters(focus)
                    }
                    sendJsonResponse(socket, """{"success":true}""")
                }
                "/api/rotation" -> {
                    val degrees = queryParams["degrees"]?.toIntOrNull() ?: 0
                    val success = callbacks.onSetRotation(degrees)
                    sendJsonResponse(socket, """{"success":$success,"rotation":$degrees}""")
                }
                "/api/auto-adjust" -> {
                    val enabled = queryParams["enabled"]?.toBooleanStrictOrNull() ?: true
                    val success = callbacks.onSetAutoAdjust(enabled)
                    sendJsonResponse(socket, """{"success":$success,"autoAdjust":$enabled}""")
                }
                "/api/filter" -> {
                    val filter = queryParams["filter"] ?: queryParams["name"] ?: "none"
                    val success = callbacks.onSetFilter(filter)
                    sendJsonResponse(socket, """{"success":$success,"filter":"${escapeJson(filter)}"}""")
                }
                "/api/tea", "/api/reward-tea" -> {
                    val success = callbacks.onRewardTeaReceived()
                    sendJsonResponse(socket, """{"success":$success,"message":"Tea reward received"}""")
                }
                else -> send404(socket)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Connection handler error: ${e.message}")
            runCatching { socket.close() }
        }
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        if (queryString.isEmpty()) return emptyMap()
        val params = mutableMapOf<String, String>()
        for (pair in queryString.split("&")) {
            val kv = pair.split("=", limit = 2)
            if (kv.isNotEmpty()) {
                val key = runCatching { URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name()) }.getOrDefault(kv[0])
                val value = if (kv.size > 1) {
                    runCatching { URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()) }.getOrDefault(kv[1])
                } else ""
                params[key] = value
            }
        }
        return params
    }

    private fun sendOptionsResponse(socket: Socket) {
        val out = BufferedOutputStream(socket.getOutputStream())
        val header = "HTTP/1.1 204 No Content\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
            "Access-Control-Allow-Headers: Content-Type\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        out.flush()
        socket.close()
    }

    private fun serveIndex(socket: Socket) {
        val html = WebStreamHtml.getIndexHtml(hostAddress, port)
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val out = BufferedOutputStream(socket.getOutputStream())
        val header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html; charset=UTF-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        out.write(bytes)
        out.flush()
        socket.close()
    }

    private fun serveObs(socket: Socket) {
        val streamUrl = "http://$hostAddress:$port/video.mjpeg"
        val audioUrl = "http://$hostAddress:$port/audio.wav"
        val html = WebStreamHtml.getObsHtml(streamUrl, audioUrl)
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val out = BufferedOutputStream(socket.getOutputStream())
        val header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html; charset=UTF-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        out.write(bytes)
        out.flush()
        socket.close()
    }

    private fun serveSnapshot(socket: Socket) {
        val jpeg = callbacks.getLatestSnapshot()
        socket.tcpNoDelay = true
        socket.trafficClass = 0x10 // IPTOS_LOWDELAY
        socket.sendBufferSize = 128 * 1024
        val out = BufferedOutputStream(socket.getOutputStream(), 64 * 1024)
        if ((jpeg == null) || jpeg.isEmpty()) {
            val err = "No snapshot available yet"
            val errBytes = err.toByteArray(StandardCharsets.UTF_8)
            val header = "HTTP/1.1 503 Service Unavailable\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${errBytes.size}\r\n" +
                "Connection: close\r\n\r\n"
            out.write(header.toByteArray(StandardCharsets.US_ASCII))
            out.write(errBytes)
            out.flush()
            socket.close()
            return
        }

        val header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "Content-Length: ${jpeg.size}\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        out.write(jpeg)
        out.flush()
        socket.close()
    }

    private fun serveLatestVideo(socket: Socket) {
        val streamPair = callbacks.openLatestVideoStream()
        if (streamPair == null) {
            send404(socket)
            return
        }
        val (stream, length) = streamPair
        try {
            val out = BufferedOutputStream(socket.getOutputStream(), 64 * 1024)
            val lengthHeader = if (length > 0) "Content-Length: $length\r\n" else ""
            val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: video/mp4\r\n" +
                "Content-Disposition: attachment; filename=\"ObsWifiCams_PhoneVideo.mp4\"\r\n" +
                lengthHeader +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
            out.write(header.toByteArray(StandardCharsets.US_ASCII))
            stream.use { input ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                }
            }
            out.flush()
        } catch (_: Exception) {
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun serveStatus(socket: Socket) {
        val info = callbacks.getStatusInfo()
        val json = toJson(info)
        sendJsonResponse(socket, json)
    }

    private fun toJson(info: StatusInfo): String {
        val lensesJson = info.lenses.joinToString(",", prefix = "[", postfix = "]") { (id, name, isFront) ->
            """{"id":"${escapeJson(id)}","name":"${escapeJson(name)}","isFront":$isFront}"""
        }
        return """{""" +
            """"streaming":${info.isStreaming},""" +
            """"clients":${info.clientCount},""" +
            """"fps":${info.fps},""" +
            """"targetFps":"${escapeJson(info.targetFps)}",""" +
            """"resolution":"${escapeJson(info.resolution)}",""" +
            """"facing":"${escapeJson(info.facing)}",""" +
            """"torch":${info.isTorchOn},""" +
            """"isTorchOn":${info.isTorchOn},""" +
            """"torchStrength":${info.torchStrength},""" +
            """"torchStrengthMax":${info.torchStrengthMax},""" +
            """"minZoom":${info.minZoom},""" +
            """"maxZoom":${info.maxZoom},""" +
            """"zoomRatio":${info.zoomRatio},""" +
            """"evMin":${info.evMin},""" +
            """"evMax":${info.evMax},""" +
            """"evIndex":${info.evIndex},""" +
            """"isRecording":${info.isRecording},""" +
            """"recordingSeconds":${info.recordingSeconds},""" +
            """"audioSupported":${info.audioSupported},""" +
            """"audioLevel":${info.audioLevel},""" +
            """"quality":"${escapeJson(info.quality)}",""" +
            """"aspect":"${escapeJson(info.aspect)}",""" +
            """"autoAdjust":${info.autoAdjust},""" +
            """"rotation":${info.rotation},""" +
            """"iso":${info.iso ?: -1},""" +
            """"awb":${info.awb},""" +
            """"focus":${info.focus ?: -1f},""" +
            """"mode":"${escapeJson(info.mode)}",""" +
            """"flash":"${escapeJson(info.flash)}",""" +
            """"filter":"${escapeJson(info.filter)}",""" +
            """"lenses":$lensesJson}"""
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun sendJsonResponse(socket: Socket, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val out = BufferedOutputStream(socket.getOutputStream())
        val header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Cache-Control: no-cache\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        out.write(bytes)
        out.flush()
        socket.close()
    }

    private fun send503(socket: Socket) {
        val message = "Audio streaming not available"
        val bytes = message.toByteArray(StandardCharsets.UTF_8)
        val out = BufferedOutputStream(socket.getOutputStream())
        val header = "HTTP/1.1 503 Service Unavailable\r\n" +
            "Content-Type: text/plain; charset=UTF-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        out.write(bytes)
        out.flush()
        socket.close()
    }

    private fun send404(socket: Socket) {
        val body = "Not Found".toByteArray(StandardCharsets.US_ASCII)
        val out = BufferedOutputStream(socket.getOutputStream())
        val header = "HTTP/1.1 404 Not Found\r\n" +
            "Content-Type: text/plain\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        out.write(body)
        out.flush()
        socket.close()
    }

    private fun serveMjpegStream(socket: Socket) {
        socket.tcpNoDelay = true
        socket.keepAlive = true
        socket.trafficClass = 0x10 // IPTOS_LOWDELAY
        socket.sendBufferSize = 256 * 1024
        val out = BufferedOutputStream(socket.getOutputStream(), 128 * 1024)
        val initHeader = "HTTP/1.1 200 OK\r\n" +
            "Server: ObsWifiCams/3.0\r\n" +
            "Connection: close\r\n" +
            "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
            "Pragma: no-cache\r\n" +
            "Expires: 0\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n\r\n"
        out.write(initHeader.toByteArray(StandardCharsets.US_ASCII))
        out.flush()

        // Send current frame immediately if available
        callbacks.getLatestSnapshot()?.let { firstFrame ->
            val firstHeader = "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${firstFrame.size}\r\n\r\n"
            out.write(firstHeader.toByteArray(StandardCharsets.US_ASCII))
            out.write(firstFrame)
            out.write(crlfBytes)
            out.flush()
        }

        val client = StreamClient(socket, out)
        activeStreamClients.add(client)
        notifyClientCount()
        Log.i(TAG, "MJPEG client connected: ${socket.inetAddress.hostAddress}. Active clients: ${activeStreamClients.size}")
    }

    private class StreamClient(
        val socket: Socket,
        val output: BufferedOutputStream,
    ) {
        val isWriting = AtomicBoolean(false)
        val isClosed = AtomicBoolean(false)

        fun close() {
            if (isClosed.compareAndSet(false, true)) {
                runCatching { socket.close() }
            }
        }
    }

    companion object {
        private const val TAG = "WifiCameraServer"
    }
}
