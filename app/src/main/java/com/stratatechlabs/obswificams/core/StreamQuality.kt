package com.stratatechlabs.obswificams.core

/**
 * Resolution and compression presets for Wi-Fi camera sharing (browser / OBS / VLC streaming).
 */
enum class StreamQuality(
    val key: String,
    val targetWidth: Int,
    val targetHeight: Int,
    val jpegQuality: Int,
    val label: String,
) {
    AUTO("auto", 1280, 720, 80, "Auto Resolution"),
    UHD_4K("4k", 3840, 2160, 85, "4K UHD (2160p)"),
    QHD_2K("2k", 2560, 1440, 80, "2K QHD (1440p)"),
    FHD_1080P("1080p", 1920, 1080, 80, "1080p FHD"),
    HD_720P("720p", 1280, 720, 75, "720p HD (Recommended)"),
    SD_480P("480p", 854, 480, 70, "480p SD");

    companion object {
        fun fromKey(key: String?): StreamQuality = entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: HD_720P
    }
}
