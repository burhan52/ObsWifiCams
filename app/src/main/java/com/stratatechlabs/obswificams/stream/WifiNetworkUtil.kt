package com.stratatechlabs.obswificams.stream

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object WifiNetworkUtil {

    /**
     * Resolves the device's local IPv4 address on Wi-Fi, hotspot (AP), or USB/Ethernet.
     */
    fun getLocalIpAddress(context: Context): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Pass 1: Look for wireless or hotspot or ethernet interfaces
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val name = intf.name.lowercase(Locale.ROOT)
                val isPreferred = name.startsWith("wlan") ||
                    name.startsWith("ap") ||
                    name.startsWith("eth") ||
                    name.startsWith("rndis") ||
                    name.startsWith("en")
                if (isPreferred) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if ((!addr.isLoopbackAddress) && (addr is Inet4Address)) {
                            val host = addr.hostAddress ?: continue
                            if (!host.startsWith("127.")) {
                                return host
                            }
                        }
                    }
                }
            }
            // Pass 2: Any non-loopback IPv4 address
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if ((!addr.isLoopbackAddress) && (addr is Inet4Address)) {
                        val host = addr.hostAddress ?: continue
                        if (!host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: WifiManager connection info
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val ip = wm?.connectionInfo?.ipAddress ?: 0
            if (ip != 0) {
                return String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    ip shr 8 and 0xff,
                    ip shr 16 and 0xff,
                    ip shr 24 and 0xff,
                )
            }
        } catch (_: Exception) {}

        return null
    }
}
