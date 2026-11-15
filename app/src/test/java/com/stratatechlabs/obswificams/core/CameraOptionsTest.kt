package com.stratatechlabs.obswificams.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraOptionsTest {

    @Test
    fun `extension modes are hidden when unsupported`() {
        val modes = CaptureMode.available(emptySet())
        assertEquals(listOf(CaptureMode.PHOTO, CaptureMode.VIDEO, CaptureMode.PRO, CaptureMode.SCAN), modes)
    }

    @Test
    fun `supported extension modes are listed before photo`() {
        val modes = CaptureMode.available(setOf(ExtensionIds.NIGHT))
        assertEquals(CaptureMode.NIGHT, modes.first())
        assertFalse(CaptureMode.PORTRAIT in modes)
    }

    @Test
    fun `dual mode is listed only with a concurrent camera pair`() {
        assertFalse(CaptureMode.DUAL in CaptureMode.available(emptySet()))
        val modes = CaptureMode.available(emptySet(), dualSupported = true)
        assertEquals(listOf(CaptureMode.PHOTO, CaptureMode.VIDEO, CaptureMode.DUAL, CaptureMode.PRO, CaptureMode.SCAN), modes)
    }

    @Test
    fun `dual records video and has no capture flash or torch`() {
        assertTrue(CaptureMode.DUAL.isVideo)
        assertTrue(CaptureMode.VIDEO.isVideo)
        assertFalse(CaptureMode.DUAL.isPhotoLike)
        assertFalse(CaptureMode.DUAL.usesTorch)
        assertFalse(CaptureMode.PHOTO.isVideo)
    }

    @Test
    fun `mode keys round trip and unknown keys fall back to photo`() {
        CaptureMode.entries.forEach { assertEquals(it, CaptureMode.fromKey(it.key)) }
        assertEquals(CaptureMode.PHOTO, CaptureMode.fromKey("bogus"))
        assertEquals(CaptureMode.PHOTO, CaptureMode.fromKey(null))
    }

    @Test
    fun `photo like modes`() {
        assertTrue(CaptureMode.PRO.isPhotoLike)
        assertTrue(CaptureMode.NIGHT.isPhotoLike)
        assertFalse(CaptureMode.VIDEO.isPhotoLike)
        assertFalse(CaptureMode.SCAN.isPhotoLike)
    }

    @Test
    fun `flash cycles off auto on in photo mode`() {
        assertEquals(FlashOption.AUTO, FlashOption.next(FlashOption.OFF, videoMode = false))
        assertEquals(FlashOption.ON, FlashOption.next(FlashOption.AUTO, videoMode = false))
        assertEquals(FlashOption.OFF, FlashOption.next(FlashOption.ON, videoMode = false))
        assertEquals(FlashOption.OFF, FlashOption.next(FlashOption.TORCH, videoMode = false))
    }

    @Test
    fun `flash toggles torch in video mode`() {
        assertEquals(FlashOption.TORCH, FlashOption.next(FlashOption.OFF, videoMode = true))
        assertEquals(FlashOption.TORCH, FlashOption.next(FlashOption.AUTO, videoMode = true))
        assertEquals(FlashOption.OFF, FlashOption.next(FlashOption.TORCH, videoMode = true))
    }

    @Test
    fun `timer and aspect cycle through every value`() {
        var t = TimerOption.OFF
        val seen = mutableListOf<TimerOption>()
        repeat(TimerOption.entries.size) { seen += t; t = t.next() }
        assertEquals(TimerOption.entries, seen)
        assertEquals(TimerOption.OFF, t)

        assertEquals(AspectOption.RATIO_16_9, AspectOption.RATIO_4_3.next())
        assertEquals(AspectOption.RATIO_4_3, AspectOption.RATIO_1_1.next())
        assertEquals(4f / 3f, AspectOption.RATIO_4_3.heightOverWidth, 1e-6f)
    }

    @Test
    fun `aspect label follows the window shape`() {
        assertEquals("3:4", AspectOption.RATIO_4_3.label(landscape = false))
        assertEquals("4:3", AspectOption.RATIO_4_3.label(landscape = true))
        assertEquals("16:9", AspectOption.RATIO_16_9.label(landscape = true))
        assertEquals("1:1", AspectOption.RATIO_1_1.label(landscape = true))
    }

    @Test
    fun `every option enum round trips its key`() {
        FlashOption.entries.forEach { assertEquals(it, FlashOption.fromKey(it.key)) }
        TimerOption.entries.forEach { assertEquals(it, TimerOption.fromKey(it.key)) }
        AspectOption.entries.forEach { assertEquals(it, AspectOption.fromKey(it.key)) }
        GridOption.entries.forEach { assertEquals(it, GridOption.fromKey(it.key)) }
        VolumeKeyAction.entries.forEach { assertEquals(it, VolumeKeyAction.fromKey(it.key)) }
        PhotoQualityOption.entries.forEach { assertEquals(it, PhotoQualityOption.fromKey(it.key)) }
        VideoQualityOption.entries.forEach { assertEquals(it, VideoQualityOption.fromKey(it.key)) }
        StreamQuality.entries.forEach { assertEquals(it, StreamQuality.fromKey(it.key)) }
        assertEquals(StreamQuality.AUTO, StreamQuality.fromKey("auto"))
        assertEquals(StreamQuality.HD_720P, StreamQuality.fromKey("unknown"))
        assertEquals(StreamQuality.HD_720P, StreamQuality.fromKey(null))
        assertEquals(StreamQuality.UHD_4K, StreamQuality.fromKey("4k"))
        assertEquals(3840, StreamQuality.UHD_4K.targetWidth)
        assertEquals(2160, StreamQuality.UHD_4K.targetHeight)
        assertEquals(StreamQuality.QHD_2K, StreamQuality.fromKey("2k"))
        assertEquals(2560, StreamQuality.QHD_2K.targetWidth)
        assertEquals(1440, StreamQuality.QHD_2K.targetHeight)
        assertEquals(1280, StreamQuality.HD_720P.targetWidth)
        assertEquals(720, StreamQuality.HD_720P.targetHeight)

        StreamFps.entries.forEach { assertEquals(it, StreamFps.fromKey(it.key)) }
        assertEquals(StreamFps.AUTO, StreamFps.fromKey("auto"))
        assertEquals(StreamFps.FPS_120, StreamFps.fromKey("120"))
        assertEquals(StreamFps.FPS_60, StreamFps.fromKey("60fps"))
        assertEquals(StreamFps.FPS_30, StreamFps.fromKey("30"))
        assertEquals(StreamFps.FPS_24, StreamFps.fromKey("24"))
        assertEquals(StreamFps.FPS_15, StreamFps.fromKey("15"))
        assertEquals(StreamFps.AUTO, StreamFps.fromKey("invalid"))
        assertEquals(StreamFps.FPS_60, StreamFps.fromFps(60))
        assertEquals(StreamFps.FPS_120, StreamFps.fromFps(120))
        assertEquals(StreamFps.FPS_30, StreamFps.fromFps(30))
        assertEquals(StreamFps.FPS_24, StreamFps.fromFps(24))
        assertEquals(StreamFps.FPS_15, StreamFps.fromFps(15))
        assertEquals(StreamFps.AUTO, StreamFps.fromFps(0))

        assertEquals(VideoQualityOption.FHD, VideoQualityOption.fromKey(null))
        assertEquals(GridOption.OFF, GridOption.fromKey("nope"))
    }
}
