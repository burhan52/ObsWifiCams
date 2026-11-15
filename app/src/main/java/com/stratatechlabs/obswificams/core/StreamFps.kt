package com.stratatechlabs.obswificams.core

/**
 * Target frame rates for Wi-Fi streaming (120 FPS down to 15 FPS, plus Auto FPS).
 */
enum class StreamFps(
    val key: String,
    @Suppress("unused") val targetFps: Int,
    val intervalMs: Long,
    val label: String,
) {
    AUTO("auto", 0, 33L, "Auto FPS"),
    FPS_120("120", 120, 8L, "120 FPS (Ultra)"),
    FPS_60("60", 60, 16L, "60 FPS (Smooth)"),
    FPS_30("30", 30, 33L, "30 FPS (Standard)"),
    FPS_24("24", 24, 41L, "24 FPS (Cinematic)"),
    FPS_15("15", 15, 66L, "15 FPS (Saver)");

    companion object {
        fun fromKey(key: String?): StreamFps = when (key?.lowercase()?.trim()) {
            "auto", "0" -> AUTO
            "120", "120fps" -> FPS_120
            "60", "60fps" -> FPS_60
            "30", "30fps" -> FPS_30
            "24", "24fps" -> FPS_24
            "15", "15fps" -> FPS_15
            else -> AUTO
        }

        fun fromFps(fps: Int): StreamFps = when (fps) {
            0 -> AUTO
            in 100..150 -> FPS_120
            in 50..99 -> FPS_60
            in 26..49 -> FPS_30
            in 19..25 -> FPS_24
            in 1..18 -> FPS_15
            else -> AUTO
        }
    }
}
