package com.stratatechlabs.obswificams.stream

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class WifiCameraServerTest {

    private var serverPort = 0
    private var server: WifiCameraServer? = null

    private val switchCalled = AtomicBoolean(false)
    private val torchCalled = AtomicBoolean(false)
    private val captureCalled = AtomicBoolean(false)
    private val recordCalled = AtomicBoolean(false)
    private val zoomRatioSet = java.util.concurrent.atomic.AtomicReference<Float?>(null)
    private val qualityKeySet = java.util.concurrent.atomic.AtomicReference<String?>(null)
    private val fpsKeySet = java.util.concurrent.atomic.AtomicReference<String?>(null)
    private val autoAdjustVal = AtomicBoolean(true)
    private val rotationSet = java.util.concurrent.atomic.AtomicInteger(0)
    private val proIsoSet = java.util.concurrent.atomic.AtomicReference<Int?>(null)
    private val proAwbSet = java.util.concurrent.atomic.AtomicReference<Int?>(null)
    private val filterNameSet = java.util.concurrent.atomic.AtomicReference<String?>(null)
    private val rewardTeaCalled = AtomicBoolean(false)
    private val sampleJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

    @Before
    fun setUp() {
        // Find free port
        val socket = ServerSocket(0)
        serverPort = socket.localPort
        socket.close()

        val callbacks = object : WifiCameraServer.ServerCallbacks {
            override fun getLatestSnapshot(): ByteArray = sampleJpeg

            override fun onSwitchCamera(): Boolean {
                switchCalled.set(true)
                return true
            }

            override fun onToggleTorch(): Boolean {
                torchCalled.set(true)
                return true
            }

            override fun onTriggerCapture(): Boolean {
                captureCalled.set(true)
                return true
            }

            override fun onToggleRecording(): Boolean {
                recordCalled.set(true)
                return true
            }

            override fun onSetZoom(ratio: Float): Boolean {
                zoomRatioSet.set(ratio)
                return true
            }

            override fun onSetQuality(qualityKey: String): Boolean {
                qualityKeySet.set(qualityKey)
                return true
            }

            override fun onSetFps(fps: String): Boolean {
                fpsKeySet.set(fps)
                return true
            }

            override fun onSetAutoAdjust(enabled: Boolean): Boolean {
                autoAdjustVal.set(enabled)
                return true
            }

            override fun onSetRotation(degrees: Int): Boolean {
                rotationSet.set(degrees)
                return true
            }

            override fun onSetProIso(iso: Int?): Boolean {
                proIsoSet.set(iso)
                return true
            }

            override fun onSetProAwb(awb: Int?): Boolean {
                proAwbSet.set(awb)
                return true
            }

            override fun onSetFilter(name: String): Boolean {
                filterNameSet.set(name)
                return true
            }

            override fun onRewardTeaReceived(): Boolean {
                rewardTeaCalled.set(true)
                return true
            }

            override fun onClientCountChanged(count: Int) = Unit

            override fun getStatusInfo(): WifiCameraServer.StatusInfo = WifiCameraServer.StatusInfo(
                isStreaming = true,
                clientCount = 1,
                fps = 30,
                resolution = "1280x720",
                facing = "back",
                isTorchOn = false,
            )
        }

        server = WifiCameraServer(serverPort, "127.0.0.1", callbacks).apply {
            start()
        }
        Thread.sleep(50)
    }

    @After
    fun tearDown() {
        server?.stop()
        server = null
    }

    @Test
    fun `root endpoint serves index html`() {
        val url = URL("http://127.0.0.1:$serverPort/")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertTrue(conn.contentType.contains("text/html"))
        val content = conn.inputStream.bufferedReader().use { it.readText() }
        assertTrue(content.contains("ObsWifiCams"))
        assertTrue(content.contains("OBS Studio"))
        conn.disconnect()
    }

    @Test
    fun `obs endpoint serves clean obs html`() {
        val url = URL("http://127.0.0.1:$serverPort/obs")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        val content = conn.inputStream.bufferedReader().use { it.readText() }
        assertTrue(content.contains("ObsWifiCams OBS Stream"))
        conn.disconnect()
    }

    @Test
    fun `snapshot endpoint returns jpeg image`() {
        val url = URL("http://127.0.0.1:$serverPort/snapshot.jpg")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertEquals("image/jpeg", conn.contentType)
        val bytes = conn.inputStream.readBytes()
        assertEquals(sampleJpeg.size, bytes.size)
        conn.disconnect()
    }

    @Test
    fun `status endpoint returns json`() {
        val url = URL("http://127.0.0.1:$serverPort/status")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertTrue(conn.contentType.contains("application/json"))
        val json = conn.inputStream.bufferedReader().use { it.readText() }
        assertTrue(json.contains("\"streaming\":true"))
        assertTrue(json.contains("\"fps\":30"))
        assertTrue(json.contains("\"resolution\":\"1280x720\""))
        conn.disconnect()
    }

    @Test
    fun `api switch-camera triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/switch-camera")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertTrue(switchCalled.get())
        conn.disconnect()
    }

    @Test
    fun `api torch triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/torch")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertTrue(torchCalled.get())
        conn.disconnect()
    }

    @Test
    fun `mjpeg stream client receives multipart frames`() {
        val client = Socket("127.0.0.1", serverPort)
        client.soTimeout = 2000
        val out = client.getOutputStream()
        out.write("GET /video.mjpeg HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
        out.flush()

        val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII))
        val statusLine = reader.readLine()
        assertEquals("HTTP/1.1 200 OK", statusLine)

        var foundContentType = false
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.contains("multipart/x-mixed-replace")) foundContentType = true
        }
        assertTrue(foundContentType)

        // Read first boundary header
        val boundary = reader.readLine()
        assertEquals("--frame", boundary)

        client.close()
    }

    @Test
    fun `api record triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/record")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertTrue(recordCalled.get())
        conn.disconnect()
    }

    @Test
    fun `api zoom parses ratio parameter and triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/zoom?ratio=2.5")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertEquals(2.5f, zoomRatioSet.get() ?: 0f, 0.01f)
        conn.disconnect()
    }

    @Test
    fun `api quality parses quality parameter and triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/quality?quality=4k")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertEquals("4k", qualityKeySet.get())
        conn.disconnect()
    }

    @Test
    fun `api fps parses fps parameter and triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/fps?fps=60")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertEquals("60", fpsKeySet.get())
        conn.disconnect()
    }

    @Test
    fun `options preflight returns 204 with cors headers`() {
        val url = URL("http://127.0.0.1:$serverPort/api/zoom")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "OPTIONS"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(204, conn.responseCode)
        assertEquals("*", conn.getHeaderField("Access-Control-Allow-Origin"))
        conn.disconnect()
    }

    @Test
    fun `api auto-adjust parses parameter and triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/auto-adjust?enabled=false")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        org.junit.Assert.assertFalse(autoAdjustVal.get())
        conn.disconnect()
    }

    @Test
    fun `api rotation parses degrees parameter and triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/rotation?degrees=90")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertEquals(90, rotationSet.get())
        conn.disconnect()
    }

    @Test
    fun `api pro-manual parses iso and awb and auto reset`() {
        val url = URL("http://127.0.0.1:$serverPort/api/pro-manual?iso=400&awb=5")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertEquals(400, proIsoSet.get())
        assertEquals(5, proAwbSet.get())
        conn.disconnect()

        val urlAuto = URL("http://127.0.0.1:$serverPort/api/pro-manual?iso=auto&awb=auto")
        val connAuto = urlAuto.openConnection() as HttpURLConnection
        connAuto.requestMethod = "POST"
        connAuto.connectTimeout = 1000
        connAuto.readTimeout = 1000
        assertEquals(200, connAuto.responseCode)
        org.junit.Assert.assertNull(proIsoSet.get())
        org.junit.Assert.assertNull(proAwbSet.get())
        connAuto.disconnect()
    }

    @Test
    fun `api filter parses name parameter and triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/filter?name=bluray")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertEquals("bluray", filterNameSet.get())
        conn.disconnect()
    }

    @Test
    fun `api tea triggers callback`() {
        val url = URL("http://127.0.0.1:$serverPort/api/tea")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        assertEquals(200, conn.responseCode)
        assertTrue(rewardTeaCalled.get())
        conn.disconnect()
    }
}
