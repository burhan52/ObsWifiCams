package com.stratatechlabs.obswificams.stream

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt

/**
 * Captures microphone audio using [AudioRecord] and streams it as live PCM WAV over HTTP.
 * Also computes real-time RMS audio levels (0..100) for remote VU meter display.
 */
class WifiAudioStreamer(private val context: Context) {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val numChannels = 1
    private val bitsPerSample = 16

    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null

    private class AudioClient(
        val socket: Socket,
        val output: BufferedOutputStream,
    ) {
        fun close() {
            runCatching { socket.close() }
        }
    }

    private val activeClients = Collections.newSetFromMap(ConcurrentHashMap<AudioClient, Boolean>())
    val clientCount: Int get() = activeClients.size

    val audioLevel = AtomicInteger(0) // 0..100
    private var audioCalcCounter = 0

    val isAudioSupported: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        if (!isAudioSupported) return
        if (isRecording.getAndSet(true)) return

        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = minBuf.coerceAtLeast(4096)

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "AudioRecord not initialized")
                isRecording.set(false)
                return
            }
            record.startRecording()
            audioRecord = record

            recordThread = Thread({
                val buffer = ByteArray(1024) // ~11.6ms chunks for ultra-low latency
                while (isRecording.get()) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        calculateAudioLevel(buffer, read)
                        broadcastAudioChunk(buffer, read)
                    } else if (read < 0) {
                        Log.w(TAG, "AudioRecord read error: $read")
                        break
                    }
                }
            }, "WifiAudioRecord-Thread").also { it.start() }

            Log.i(TAG, "Audio capture started at $sampleRate Hz")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
            isRecording.set(false)
        }
    }

    private fun stopAudioCapture() {
        if (!isRecording.getAndSet(false)) return

        recordThread?.interrupt()
        recordThread = null

        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null
        audioLevel.set(0)
        Log.i(TAG, "Audio capture stopped")
    }

    private fun calculateAudioLevel(buffer: ByteArray, length: Int) {
        // Calculate audio RMS level periodically (~10 times per second) to save CPU
        if (++audioCalcCounter % 8 != 0) return
        var sumSquares = 0.0
        val sampleCount = length / 2
        for (i in 0 until length step 2) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val normalized = sample.toShort().toDouble() / 32768.0
            sumSquares += normalized * normalized
        }
        val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount) else 0.0
        val level = (rms * 100.0).toInt().coerceIn(0, 100)
        audioLevel.set(level)
    }

    private fun broadcastAudioChunk(buffer: ByteArray, length: Int) {
        if (activeClients.isEmpty()) {
            stopAudioCapture()
            return
        }

        val iterator = activeClients.iterator()
        while (iterator.hasNext()) {
            val client = iterator.next()
            try {
                client.output.write(buffer, 0, length)
                client.output.flush()
            } catch (_: IOException) {
                client.close()
                iterator.remove()
            }
        }
    }

    fun handleAudioClient(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.trafficClass = 0x10 // IPTOS_LOWDELAY
            socket.sendBufferSize = 64 * 1024
            val out = BufferedOutputStream(socket.getOutputStream(), 16 * 1024)
            val header = "HTTP/1.1 200 OK\r\n" +
                "Server: ObsWifiCams/3.0\r\n" +
                "Content-Type: audio/x-wav\r\n" +
                "Connection: close\r\n" +
                "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                "Pragma: no-cache\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n"
            out.write(header.toByteArray(StandardCharsets.US_ASCII))

            val wavHeader = createWavHeader()
            out.write(wavHeader)
            out.flush()

            val client = AudioClient(socket, out)
            activeClients.add(client)
            startAudioCapture()
            Log.i(TAG, "Audio client connected: ${socket.inetAddress.hostAddress}. Active clients: ${activeClients.size}")
        } catch (e: Exception) {
            Log.w(TAG, "Audio client setup failed", e)
            runCatching { socket.close() }
        }
    }

    private fun createWavHeader(): ByteArray {
        val totalDataLen = 0x7FFFFFFF - 36
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = (numChannels * bitsPerSample / 8).toShort()

        val buf = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(StandardCharsets.US_ASCII))
        buf.putInt(0x7FFFFFFF) // indefinite file length
        buf.put("WAVE".toByteArray(StandardCharsets.US_ASCII))
        buf.put("fmt ".toByteArray(StandardCharsets.US_ASCII))
        buf.putInt(16) // Subchunk1Size for PCM
        buf.putShort(1) // AudioFormat 1 = PCM
        buf.putShort(numChannels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort(blockAlign)
        buf.putShort(bitsPerSample.toShort())
        buf.put("data".toByteArray(StandardCharsets.US_ASCII))
        buf.putInt(totalDataLen)
        return buf.array()
    }

    fun retryAudioCaptureIfClientsActive() {
        if (activeClients.isNotEmpty() && !isRecording.get() && isAudioSupported) {
            Log.i(TAG, "Retrying audio capture after permission grant. Active clients: ${activeClients.size}")
            startAudioCapture()
        }
    }

    fun close() {
        stopAudioCapture()
        activeClients.forEach { it.close() }
        activeClients.clear()
    }

    companion object {
        private const val TAG = "WifiAudioStreamer"
    }
}
