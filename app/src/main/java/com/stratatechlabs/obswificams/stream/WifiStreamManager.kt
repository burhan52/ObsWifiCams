package com.stratatechlabs.obswificams.stream

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import com.stratatechlabs.obswificams.camera.CameraController
import com.stratatechlabs.obswificams.camera.RecordingStatus
import com.stratatechlabs.obswificams.core.AspectOption
import com.stratatechlabs.obswificams.core.CaptureMode
import com.stratatechlabs.obswificams.core.FlashOption
import com.stratatechlabs.obswificams.core.StreamFps
import com.stratatechlabs.obswificams.core.StreamQuality
import com.stratatechlabs.obswificams.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

enum class StreamFilter(val key: String) {
    NONE("none"),
    BLURAY("bluray"),
    TEAL_ORANGE("tealorange"),
    VIVID("vivid"),
    VINTAGE("vintage"),
    NOIR("noir"),
    CYBERPUNK("cyberpunk"),
    INVERT("invert");

    companion object {
        fun fromKey(key: String): StreamFilter =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: NONE
    }
}

data class WifiStreamState(
    val isStreaming: Boolean = false,
    val ipAddress: String? = null,
    val port: Int = WifiStreamManager.DEFAULT_PORT,
    val clientCount: Int = 0,
    val fps: Int = 0,
    val quality: StreamQuality = StreamQuality.HD_720P,
    val effectiveQuality: StreamQuality = StreamQuality.HD_720P,
    val targetFps: StreamFps = StreamFps.AUTO,
    val isTorchOn: Boolean = false,
    val facing: String = "back",
    val autoAdjust: Boolean = true,
    val rotation: Int = 0,
    val filter: StreamFilter = StreamFilter.NONE,
)

/**
 * Manages the Wi-Fi streaming pipeline: binds camera frames, encodes them to JPEG,
 * feeds the [WifiCameraServer], coordinates live audio streaming via [WifiAudioStreamer],
 * and dynamically auto-adjusts bitrate/resolution for smooth, lag-free streaming over Wi-Fi.
 */
class WifiStreamManager(
    private val context: Context,
    private val controller: CameraController,
    private val settings: AppSettings,
    private val onSwitchCameraRequested: () -> Unit,
    private val onTriggerCaptureRequested: () -> Unit,
    private val onTriggerRecordingRequested: (() -> Unit)? = null,
    private val onSelectModeRequested: ((CaptureMode) -> Unit)? = null,
    private val onRewardTeaRequested: (() -> Unit)? = null,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val encodeExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _state = MutableStateFlow(
        WifiStreamState(
            port = settings.wifiSharePort,
            quality = settings.wifiShareQuality,
            effectiveQuality = if (settings.wifiShareQuality == StreamQuality.AUTO) StreamQuality.HD_720P else settings.wifiShareQuality,
            targetFps = settings.wifiShareFps,
            autoAdjust = settings.wifiShareAutoAdjust,
        ),
    )
    val state: StateFlow<WifiStreamState> = _state.asStateFlow()

    val hasFlashUnit: Boolean
        get() = controller.state.value.hasFlashUnit

    private var server: WifiCameraServer? = null
    private var audioStreamer: WifiAudioStreamer? = null
    private val latestSnapshot = AtomicReference<ByteArray?>(null)

    @Volatile
    var latestVideoUri: Uri? = null

    private var targetQuality: StreamQuality = settings.wifiShareQuality
    private var effectiveQuality: StreamQuality = if (settings.wifiShareQuality == StreamQuality.AUTO) StreamQuality.HD_720P else settings.wifiShareQuality
    private var targetFps: StreamFps = settings.wifiShareFps
    private val effectiveJpegQuality = AtomicInteger(effectiveQuality.jpegQuality)
    private var streamRotationDegrees = 0
    private var currentFilter = StreamFilter.NONE

    private var lastFrameTimeMs = 0L
    private val targetFrameIntervalMs = AtomicLong(settings.wifiShareFps.intervalMs)
    private val currentFrameIntervalMs = AtomicLong(settings.wifiShareFps.intervalMs)

    private val reusableBaos = ByteArrayOutputStream(128 * 1024)

    private var congestionTicks = 0
    private var smoothTicks = 0

    private var framesThisSecond = AtomicInteger(0)
    private var lastFpsCalcTime = SystemClock.uptimeMillis()
    private val isEncoding = AtomicBoolean(false)

    private fun queryLatestVideoUri(): Uri? = runCatching {
        val collection = com.stratatechlabs.obswificams.gallery.MediaStoreHelper.videoCollection()
        val projection = arrayOf(android.provider.MediaStore.MediaColumns._ID)
        val sortOrder = "${android.provider.MediaStore.MediaColumns.DATE_ADDED} DESC"
        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID))
                android.content.ContentUris.withAppendedId(collection, id).also {
                    latestVideoUri = it
                }
            } else null
        }
    }.getOrNull()

    fun startStreaming(port: Int = settings.wifiSharePort, quality: StreamQuality = settings.wifiShareQuality): Boolean {
        if (_state.value.isStreaming) return true

        val ip = WifiNetworkUtil.getLocalIpAddress(context)
        val audio = WifiAudioStreamer(context)
        audioStreamer = audio

        targetQuality = quality
        val eff = if (quality == StreamQuality.AUTO) StreamQuality.HD_720P else quality
        effectiveQuality = eff
        effectiveJpegQuality.set(eff.jpegQuality)
        targetFps = settings.wifiShareFps
        targetFrameIntervalMs.set(targetFps.intervalMs)
        currentFrameIntervalMs.set(targetFps.intervalMs)
        congestionTicks = 0
        smoothTicks = 0

        val serverCallbacks = object : WifiCameraServer.ServerCallbacks {
            override fun getLatestSnapshot(): ByteArray? = latestSnapshot.get()

            @Suppress("Recycle")
            override fun openLatestVideoStream(): Pair<InputStream, Long>? {
                val uri = latestVideoUri ?: queryLatestVideoUri() ?: return null
                return try {
                    val cr = context.contentResolver
                    val pfd = cr.openFileDescriptor(uri, "r")
                    val size = pfd?.statSize ?: -1L
                    pfd?.close()
                    val stream = cr.openInputStream(uri) ?: return null
                    stream to size
                } catch (_: Exception) {
                    null
                }
            }

            override fun onSwitchCamera(): Boolean {
                mainHandler.post { onSwitchCameraRequested() }
                return true
            }

            override fun onSelectLens(lensId: String): Boolean {
                mainHandler.post { controller.selectCamera(lensId) }
                return true
            }

            override fun onSelectMode(mode: String): Boolean {
                val m = CaptureMode.fromKey(mode)
                mainHandler.post {
                    if (onSelectModeRequested != null) {
                        onSelectModeRequested.invoke(m)
                    } else {
                        controller.setMode(m)
                    }
                }
                return true
            }

            override fun onToggleTorch(): Boolean {
                var newState = false
                val latch = java.util.concurrent.CountDownLatch(1)
                mainHandler.post {
                    newState = controller.toggleTorch()
                    _state.update { it.copy(isTorchOn = newState) }
                    latch.countDown()
                }
                runCatching { latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS) }
                return newState
            }

            override fun onSetTorchStrength(level: Int): Boolean {
                mainHandler.post { controller.setTorchStrength(level) }
                return true
            }

            override fun onTriggerCapture(): Boolean {
                mainHandler.post { onTriggerCaptureRequested() }
                return true
            }

            override fun onToggleRecording(): Boolean {
                var isRec = false
                val latch = java.util.concurrent.CountDownLatch(1)
                mainHandler.post {
                    if (onTriggerRecordingRequested != null) {
                        onTriggerRecordingRequested.invoke()
                    } else {
                        onTriggerCaptureRequested()
                    }
                    isRec = controller.state.value.recording != RecordingStatus.IDLE
                    latch.countDown()
                }
                runCatching { latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS) }
                return isRec
            }

            override fun onSetZoom(ratio: Float): Boolean {
                mainHandler.post { controller.setZoomRatio(ratio) }
                return true
            }

            override fun onSetExposure(index: Int): Boolean {
                mainHandler.post { controller.setExposureIndex(index) }
                return true
            }

            override fun onSetQuality(qualityKey: String): Boolean {
                val q = StreamQuality.fromKey(qualityKey)
                mainHandler.post { setQuality(q) }
                return true
            }

            override fun onSetFps(fps: String): Boolean {
                val f = StreamFps.fromKey(fps)
                mainHandler.post { setFps(f) }
                return true
            }

            override fun onSetAutoAdjust(enabled: Boolean): Boolean {
                mainHandler.post { setAutoAdjust(enabled) }
                return true
            }

            override fun onSetRotation(degrees: Int): Boolean {
                mainHandler.post { setRotation(degrees) }
                return true
            }

            override fun onSetAspectRatio(aspect: String): Boolean {
                val option = when (aspect.trim().lowercase()) {
                    "16:9", "16_9", "9:16" -> AspectOption.RATIO_16_9
                    "1:1", "1_1" -> AspectOption.RATIO_1_1
                    else -> AspectOption.RATIO_4_3
                }
                mainHandler.post { controller.setAspect(option) }
                return true
            }

            override fun onSetFlash(mode: String): Boolean {
                val option = FlashOption.fromKey(mode)
                mainHandler.post { controller.setFlash(option) }
                return true
            }

            override fun onSetProIso(iso: Int?): Boolean {
                mainHandler.post { controller.setProIso(iso) }
                return true
            }

            override fun onSetProAwb(awb: Int?): Boolean {
                mainHandler.post { controller.setProAwb(awb) }
                return true
            }

            override fun onSetProFocusDiopters(diopters: Float?): Boolean {
                mainHandler.post { controller.setProFocusDiopters(diopters) }
                return true
            }

            override fun onSetFilter(name: String): Boolean {
                val f = StreamFilter.fromKey(name)
                mainHandler.post { setFilter(f) }
                return true
            }

            override fun onRewardTeaReceived(): Boolean {
                mainHandler.post { onRewardTeaRequested?.invoke() }
                return true
            }

            override fun onClientCountChanged(count: Int) {
                _state.update { it.copy(clientCount = count) }
            }

            @Suppress("DestructuringDeclaration")
            override fun getStatusInfo(): WifiCameraServer.StatusInfo {
                val (isStreaming, _, _, clientCount, fps, quality, effQuality, tfps, isTorchOn, _, autoAdjust, rotation) = _state.value
                val camState = controller.state.value
                val currentAudio = audioStreamer
                val lensesList = camState.lenses.map { lens ->
                    val focalStr = lens.equivalentFocalMm?.let { " (${it}mm)" } ?: ""
                    WifiCameraServer.LensInfo(
                        id = lens.cameraId,
                        name = "${lens.category.name.lowercase().replaceFirstChar { it.uppercase() }}$focalStr",
                        isFront = lens.isFront,
                    )
                }
                val recSec = (camState.recordingNanos / 1_000_000_000L).toInt()

                return WifiCameraServer.StatusInfo(
                    isStreaming = isStreaming,
                    clientCount = clientCount,
                    fps = fps,
                    targetFps = tfps.key,
                    resolution = "${effQuality.targetWidth}x${effQuality.targetHeight}",
                    facing = if (camState.isFront) "front" else "back",
                    isTorchOn = isTorchOn || (camState.flash == FlashOption.TORCH),
                    torchStrength = camState.torchStrength,
                    torchStrengthMax = camState.torchStrengthMax,
                    minZoom = camState.minZoom,
                    maxZoom = camState.maxZoom,
                    zoomRatio = camState.zoomRatio,
                    evMin = camState.evMin,
                    evMax = camState.evMax,
                    evIndex = camState.evIndex,
                    isRecording = camState.recording != RecordingStatus.IDLE,
                    recordingSeconds = recSec,
                    audioSupported = currentAudio?.isAudioSupported == true,
                    audioLevel = currentAudio?.audioLevel?.get() ?: 0,
                    quality = quality.key,
                    aspect = when (camState.aspect) {
                        AspectOption.RATIO_16_9 -> "16:9"
                        AspectOption.RATIO_1_1 -> "1:1"
                        AspectOption.RATIO_4_3 -> "4:3"
                    },
                    autoAdjust = autoAdjust,
                    rotation = rotation,
                    iso = camState.manual.iso,
                    awb = camState.manual.awbMode,
                    focus = camState.manual.focusDiopters,
                    mode = camState.mode.name.lowercase(),
                    flash = camState.flash.name.lowercase(),
                    filter = currentFilter.key,
                    lenses = lensesList,
                )
            }
        }

        val s = WifiCameraServer(port, ip ?: "127.0.0.1", serverCallbacks, audio)
        return try {
            s.start()
            server = s

            _state.update {
                it.copy(
                    isStreaming = true,
                    ipAddress = ip,
                    port = port,
                    quality = quality,
                    effectiveQuality = eff,
                    targetFps = targetFps,
                    facing = if (controller.state.value.isFront) "front" else "back",
                )
            }

            val hwQuality = if (quality == StreamQuality.AUTO) StreamQuality.FHD_1080P else quality
            controller.streamQuality = hwQuality
            controller.setWifiFrameConsumer { proxy -> onCameraFrame(proxy) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming server on port $port", e)
            runCatching { s.stop() }
            audio.close()
            audioStreamer = null
            false
        }
    }

    fun stopStreaming() {
        if (!_state.value.isStreaming) return

        controller.setWifiFrameConsumer(null)

        audioStreamer?.close()
        audioStreamer = null

        server?.stop()
        server = null

        _state.update {
            it.copy(
                isStreaming = false,
                clientCount = 0,
                fps = 0,
            )
        }
    }

    fun onAudioPermissionGranted() {
        audioStreamer?.retryAudioCaptureIfClientsActive()
    }

    fun setQuality(quality: StreamQuality) {
        targetQuality = quality
        settings.wifiShareQuality = quality
        val newEffective = if (quality == StreamQuality.AUTO) StreamQuality.HD_720P else quality
        effectiveQuality = newEffective
        effectiveJpegQuality.set(newEffective.jpegQuality)
        _state.update { it.copy(quality = quality, effectiveQuality = newEffective) }
        val hwQuality = if (quality == StreamQuality.AUTO) StreamQuality.FHD_1080P else quality
        if (controller.streamQuality != hwQuality) {
            controller.streamQuality = hwQuality
        }
    }

    fun setFps(fps: StreamFps) {
        targetFps = fps
        settings.wifiShareFps = fps
        targetFrameIntervalMs.set(fps.intervalMs)
        currentFrameIntervalMs.set(fps.intervalMs)
        _state.update { it.copy(targetFps = fps) }
    }

    @Suppress("unused")
    fun setFps(fpsKey: String) {
        setFps(StreamFps.fromKey(fpsKey))
    }

    fun setAutoAdjust(enabled: Boolean) {
        settings.wifiShareAutoAdjust = enabled
        if (!enabled) {
            val baseQuality = if (targetQuality == StreamQuality.AUTO) StreamQuality.HD_720P else targetQuality
            effectiveQuality = baseQuality
            effectiveJpegQuality.set(baseQuality.jpegQuality)
            currentFrameIntervalMs.set(targetFrameIntervalMs.get())
            _state.update { it.copy(effectiveQuality = baseQuality, autoAdjust = false) }
        } else {
            _state.update { it.copy(autoAdjust = true) }
        }
    }

    fun setRotation(degrees: Int) {
        val normalized = ((degrees % 360) + 360) % 360
        streamRotationDegrees = normalized
        _state.update { it.copy(rotation = normalized) }
    }

    fun setFilter(filter: StreamFilter) {
        currentFilter = filter
        _state.update { it.copy(filter = filter) }
    }

    fun toggleTorch(): Boolean {
        var newState = false
        val latch = java.util.concurrent.CountDownLatch(1)
        mainHandler.post {
            newState = controller.toggleTorch()
            _state.update { it.copy(isTorchOn = newState) }
            latch.countDown()
        }
        runCatching { latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS) }
        return newState
    }

    fun setTorch(enable: Boolean): Boolean {
        var newState = false
        val latch = java.util.concurrent.CountDownLatch(1)
        mainHandler.post {
            newState = controller.enableTorch(enable)
            _state.update { it.copy(isTorchOn = newState) }
            latch.countDown()
        }
        runCatching { latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS) }
        return newState
    }

    @Suppress("unused")
    fun setFilter(filterKey: String) {
        val f = StreamFilter.fromKey(filterKey)
        currentFilter = f
        _state.update { it.copy(filter = f) }
    }

    @Suppress("unused")
    fun getFilter(): StreamFilter = currentFilter

    @Suppress("unused")
    fun setPort(port: Int) {
        settings.wifiSharePort = port
        _state.update { it.copy(port = port) }
        if (_state.value.isStreaming) {
            stopStreaming()
            startStreaming(port, _state.value.quality)
        }
    }

    fun refreshIpAddress() {
        val ip = WifiNetworkUtil.getLocalIpAddress(context)
        _state.update { it.copy(ipAddress = ip) }
    }

    fun close() {
        stopStreaming()
        encodeExecutor.shutdownNow()
    }

    /**
     * Called when a new [ImageProxy] is delivered by CameraX's analysis use-case.
     */
    private fun onCameraFrame(proxy: ImageProxy) {
        val currentServer = server
        if ((currentServer == null) || ((!currentServer.hasClients()) && (latestSnapshot.get() != null))) {
            return
        }

        val now = SystemClock.uptimeMillis()
        val interval = if (_state.value.autoAdjust || (targetFps == StreamFps.AUTO)) currentFrameIntervalMs.get() else targetFrameIntervalMs.get()
        if ((now - lastFrameTimeMs) < interval) {
            return
        }

        // Prevent thread queue backlog if compression takes longer than frame interval
        if (!isEncoding.compareAndSet(false, true)) {
            return
        }
        lastFrameTimeMs = now

        try {
            val rotation = (proxy.imageInfo.rotationDegrees + streamRotationDegrees) % 360
            val isFront = controller.state.value.isFront
            val mirror = isFront && settings.mirrorFront
            val quality = effectiveQuality
            val compressionQuality = if (_state.value.autoAdjust) {
                effectiveJpegQuality.get()
            } else {
                quality.jpegQuality
            }

            // Convert ImageProxy to Bitmap synchronously while proxy is valid
            val rawBitmap = proxy.toBitmap()

            encodeExecutor.execute {
                try {
                    val finalBitmap = transformBitmap(rawBitmap, rotation, mirror, quality, currentFilter)

                    reusableBaos.reset()
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, reusableBaos)
                    if (finalBitmap !== rawBitmap) finalBitmap.recycle()
                    rawBitmap.recycle()

                    val jpegBytes = reusableBaos.toByteArray()
                    latestSnapshot.set(jpegBytes)
                    currentServer.broadcastFrame(jpegBytes)

                    updateFps(currentServer)
                } catch (e: Exception) {
                    Log.w(TAG, "Frame encode error", e)
                } finally {
                    isEncoding.set(false)
                }
            }
        } catch (e: Exception) {
            isEncoding.set(false)
            Log.w(TAG, "ImageProxy conversion error", e)
        }
    }

    /**
     * Fallback for modes where ImageAnalysis cannot be bound: encode an existing [Bitmap].
     */
    @Suppress("unused")
    fun onBitmapFrame(bitmap: Bitmap) {
        val currentServer = server ?: return
        if ((!currentServer.hasClients()) && (latestSnapshot.get() != null)) return

        val now = SystemClock.uptimeMillis()
        val interval = if (_state.value.autoAdjust || (targetFps == StreamFps.AUTO)) currentFrameIntervalMs.get() else targetFrameIntervalMs.get()
        if ((now - lastFrameTimeMs) < interval) return

        if (!isEncoding.compareAndSet(false, true)) return
        lastFrameTimeMs = now

        val quality = effectiveQuality
        val compressionQuality = if (_state.value.autoAdjust) {
            effectiveJpegQuality.get()
        } else {
            quality.jpegQuality
        }
        val rotation = streamRotationDegrees % 360

        encodeExecutor.execute {
            try {
                val finalBitmap = transformBitmap(bitmap, rotation, mirror = false, quality, currentFilter)

                reusableBaos.reset()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, reusableBaos)
                if (finalBitmap !== bitmap) finalBitmap.recycle()

                val jpegBytes = reusableBaos.toByteArray()
                latestSnapshot.set(jpegBytes)
                currentServer.broadcastFrame(jpegBytes)
                updateFps(currentServer)
            } catch (e: Exception) {
                Log.w(TAG, "Bitmap frame encode error", e)
            } finally {
                isEncoding.set(false)
            }
        }
    }

    /**
     * Efficient single-pass orientation, mirroring, downscaling, and optional color filtering.
     * Guarantees output fits within the target [quality] bounds to minimize compression overhead and bandwidth.
     */
    private fun transformBitmap(
        src: Bitmap,
        rotation: Int,
        mirror: Boolean,
        quality: StreamQuality,
        filter: StreamFilter,
    ): Bitmap {
        val srcW = src.width
        val srcH = src.height

        val isSwapped = (rotation == 90) || (rotation == 270)
        val naturalW = if (isSwapped) srcH else srcW
        val naturalH = if (isSwapped) srcW else srcH

        val maxW = if (isSwapped) quality.targetHeight else quality.targetWidth
        val maxH = if (isSwapped) quality.targetWidth else quality.targetHeight

        val scale = if ((naturalW > maxW) || (naturalH > maxH)) {
            minOf(maxW.toFloat() / naturalW, maxH.toFloat() / naturalH)
        } else {
            1.0f
        }

        val needsTransform = (rotation != 0) || mirror || (scale < 1.0f)

        val transformedBitmap = if (needsTransform) {
            val matrix = Matrix()
            if (mirror) matrix.preScale(-1f, 1f)
            if (rotation != 0) matrix.postRotate(rotation.toFloat())
            if (scale < 1.0f) matrix.postScale(scale, scale)
            Bitmap.createBitmap(src, 0, 0, srcW, srcH, matrix, true)
        } else {
            src
        }

        return if (filter != StreamFilter.NONE) {
            val filtered = applyFilter(transformedBitmap, filter)
            if (transformedBitmap !== src) transformedBitmap.recycle()
            filtered
        } else {
            transformedBitmap
        }
    }

    private fun applyFilter(bitmap: Bitmap, filter: StreamFilter): Bitmap {
        if (filter == StreamFilter.NONE) return bitmap
        val matrix = ColorMatrix()
        when (filter) {
            StreamFilter.BLURAY -> {
                // High-fidelity Blu-Ray remaster: Deep blacks, crisp contrast, vibrant saturation,
                // and a subtle cool film tone while preserving natural skin tones.
                val sat = ColorMatrix().apply { setSaturation(1.18f) }
                val filmTone = ColorMatrix(
                    floatArrayOf(
                        1.08f,  0.00f, -0.02f, 0f, -8f,
                        0.00f,  1.08f,  0.00f, 0f, -8f,
                        -0.02f, 0.02f,  1.16f, 0f, -2f,
                        0.00f,  0.00f,  0.00f, 1f,  0f,
                    ),
                )
                matrix.setConcat(filmTone, sat)
            }
            StreamFilter.TEAL_ORANGE -> {
                // Hollywood blockbuster: warm golden skin tones & highlights, deep teal/cyan shadows.
                val sat = ColorMatrix().apply { setSaturation(1.22f) }
                val grade = ColorMatrix(
                    floatArrayOf(
                        1.18f,  0.06f, -0.12f, 0f,  6f,
                        -0.04f, 1.10f,  0.04f, 0f, -6f,
                        -0.16f, 0.08f,  1.26f, 0f,  8f,
                        0.00f,  0.00f,  0.00f, 1f,  0f,
                    ),
                )
                matrix.setConcat(grade, sat)
            }
            StreamFilter.NOIR -> {
                // Classic Film Noir: Rich B&W with dramatic high contrast and deep shadows.
                val sat = ColorMatrix().apply { setSaturation(0f) }
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.30f, 0f,    0f,    0f, -28f,
                        0f,    1.30f, 0f,    0f, -28f,
                        0f,    0f,    1.30f, 0f, -28f,
                        0f,    0f,    0f,    1f,  0f,
                    ),
                )
                matrix.setConcat(contrast, sat)
            }
            StreamFilter.VIVID -> {
                // Commercial vivid pop: Enhanced saturation and micro-contrast.
                val sat = ColorMatrix().apply { setSaturation(1.45f) }
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.10f, 0f,    0f,    0f, -10f,
                        0f,    1.10f, 0f,    0f, -10f,
                        0f,    0f,    1.10f, 0f, -10f,
                        0f,    0f,    0f,    1f,  0f,
                    ),
                )
                matrix.setConcat(contrast, sat)
            }
            StreamFilter.VINTAGE -> {
                // Nostalgic analog film: warm golden highlights, softened shadows, gentle sepia warmth.
                val sat = ColorMatrix().apply { setSaturation(0.88f) }
                val film = ColorMatrix(
                    floatArrayOf(
                        1.06f, 0.08f, 0.02f, 0f, 14f,
                        0.04f, 1.00f, 0.02f, 0f,  6f,
                        0.02f, 0.04f, 0.86f, 0f, 18f,
                        0.00f, 0.00f, 0.00f, 1f,  0f,
                    ),
                )
                matrix.setConcat(film, sat)
            }
            StreamFilter.CYBERPUNK -> {
                // Futuristic neon night: electric magenta highlights & deep cyan shadows.
                val sat = ColorMatrix().apply { setSaturation(1.40f) }
                val neon = ColorMatrix(
                    floatArrayOf(
                        1.30f, -0.08f,  0.15f, 0f,  16f,
                        -0.08f, 0.96f,  0.04f, 0f, -12f,
                        0.08f, -0.04f,  1.38f, 0f,  20f,
                        0.00f,  0.00f,  0.00f, 1f,   0f,
                    ),
                )
                matrix.setConcat(neon, sat)
            }
            StreamFilter.INVERT -> {
                matrix.set(
                    floatArrayOf(
                        -1f, 0f,  0f,  0f, 255f,
                        0f,  -1f, 0f,  0f, 255f,
                        0f,  0f,  -1f, 0f, 255f,
                        0f,  0f,  0f,  1f,   0f,
                    ),
                )
            }
            StreamFilter.NONE -> return bitmap
        }

        val filtered = createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filtered)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return filtered
    }

    private fun updateFps(currentServer: WifiCameraServer) {
        val count = framesThisSecond.incrementAndGet()
        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastFpsCalcTime
        if (elapsed >= 1000) {
            val fps = ((count * 1000L) / elapsed).toInt()
            framesThisSecond.set(0)
            lastFpsCalcTime = now
            _state.update { it.copy(fps = fps) }

            checkAdaptiveSmoothing(currentServer)
        }
    }

    /**
     * Monitors socket drop rate and adjusts compression quality, frame rate, and resolution dynamically
     * to keep Wi-Fi streaming perfectly smooth without stutter.
     */
    private fun checkAdaptiveSmoothing(currentServer: WifiCameraServer) {
        if (!_state.value.autoAdjust || !currentServer.hasClients()) return

        val (delivered, dropped) = currentServer.getAndResetDropStats()
        val total = delivered + dropped
        if (total == 0) return

        val dropRate = dropped.toFloat() / total.toFloat()
        val isAutoFps = (targetFps == StreamFps.AUTO)

        if ((dropRate > 0.15f) || (dropped > 2)) {
            // Bottleneck or socket lag detected
            smoothTicks = 0
            congestionTicks++

            // Tier 1: lower JPEG quality to instantly reduce frame byte size by up to 60%
            val currentQ = effectiveJpegQuality.get()
            if (currentQ > 45) {
                effectiveJpegQuality.set((currentQ - 10).coerceAtLeast(45))
                Log.d(TAG, "Auto-adjust: lowering JPEG quality to ${effectiveJpegQuality.get()} due to dropRate ${(dropRate * 100).toInt()}%")
            }

            // Tier 2: frame rate throttling for lag reduction
            if (isAutoFps || (congestionTicks >= 2)) {
                val curInterval = currentFrameIntervalMs.get()
                if (curInterval < 41L) {
                    currentFrameIntervalMs.set(41L) // ~24 FPS
                } else if ((congestionTicks >= 2) && (curInterval < 66L)) {
                    currentFrameIntervalMs.set(66L) // ~15 FPS
                }
            }

            // Tier 3: sustained congestion over 3 seconds -> step down stream resolution
            if (congestionTicks >= 3) {
                val curEff = effectiveQuality
                val nextLower = when (curEff) {
                    StreamQuality.UHD_4K -> StreamQuality.QHD_2K
                    StreamQuality.QHD_2K -> StreamQuality.FHD_1080P
                    StreamQuality.FHD_1080P -> StreamQuality.HD_720P
                    StreamQuality.HD_720P -> StreamQuality.SD_480P
                    StreamQuality.SD_480P, StreamQuality.AUTO -> null
                }
                if (nextLower != null) {
                    Log.i(TAG, "Auto-adjust: sustained congestion, downscaling from ${curEff.label} to ${nextLower.label}")
                    effectiveQuality = nextLower
                    effectiveJpegQuality.set(nextLower.jpegQuality)
                    _state.update { it.copy(effectiveQuality = nextLower) }
                }
                congestionTicks = 0
            }
        } else {
            // Flawless transmission
            congestionTicks = 0
            smoothTicks++

            // After 4 smooth seconds, gradually restore compression quality
            if (smoothTicks >= 4) {
                val targetQ = effectiveQuality.jpegQuality
                val currentQ = effectiveJpegQuality.get()
                if (currentQ < targetQ) {
                    effectiveJpegQuality.set((currentQ + 5).coerceAtMost(targetQ))
                }

                // Restore frame rate towards target FPS
                if (isAutoFps) {
                    val curInterval = currentFrameIntervalMs.get()
                    if (curInterval > 33L) {
                        currentFrameIntervalMs.set(33L) // ~30 FPS
                    } else if ((smoothTicks >= 8) && (curInterval > 16L)) {
                        currentFrameIntervalMs.set(16L) // ~60 FPS when connection is flawless!
                    }
                } else {
                    val baseTarget = targetFrameIntervalMs.get()
                    if (currentFrameIntervalMs.get() > baseTarget) {
                        currentFrameIntervalMs.set(baseTarget)
                    }
                }
            }

            // After 10 smooth seconds, restore resolution step-by-step towards user target
            if (smoothTicks >= 10) {
                val curEff = effectiveQuality
                val maxTarget = if (targetQuality == StreamQuality.AUTO) StreamQuality.FHD_1080P else targetQuality
                if (curEff != maxTarget) {
                    val nextHigher = when (curEff) {
                        StreamQuality.SD_480P -> StreamQuality.HD_720P
                        StreamQuality.HD_720P -> if (maxTarget == StreamQuality.FHD_1080P || maxTarget == StreamQuality.QHD_2K || maxTarget == StreamQuality.UHD_4K) StreamQuality.FHD_1080P else null
                        StreamQuality.FHD_1080P -> if (maxTarget == StreamQuality.QHD_2K || maxTarget == StreamQuality.UHD_4K) StreamQuality.QHD_2K else null
                        StreamQuality.QHD_2K -> if (maxTarget == StreamQuality.UHD_4K) StreamQuality.UHD_4K else null
                        StreamQuality.UHD_4K, StreamQuality.AUTO -> null
                    }
                    if (nextHigher != null) {
                        Log.i(TAG, "Auto-adjust: Wi-Fi stable, scaling up resolution to ${nextHigher.label}")
                        effectiveQuality = nextHigher
                        effectiveJpegQuality.set(nextHigher.jpegQuality)
                        _state.update { it.copy(effectiveQuality = nextHigher) }
                    }
                }
                smoothTicks = 0
            }
        }
    }

    companion object {
        private const val TAG = "WifiStreamManager"
        const val DEFAULT_PORT = 8080
    }
}
