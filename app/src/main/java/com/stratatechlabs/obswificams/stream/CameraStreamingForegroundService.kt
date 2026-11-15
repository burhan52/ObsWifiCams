package com.stratatechlabs.obswificams.stream

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stratatechlabs.obswificams.R
import com.stratatechlabs.obswificams.ui.CameraActivity

/**
 * Foreground Service that keeps CPU, Wi-Fi, and Camera alive when the device sleeps or locks,
 * allowing live camera streaming and video recording to continue seamlessly in the background.
 */
class CameraStreamingForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        val streamUrl = intent?.getStringExtra(EXTRA_STREAM_URL) ?: ""
        val isRecording = intent?.getBooleanExtra(EXTRA_IS_RECORDING, false) ?: false

        acquireLocks()
        startForegroundWithNotification(streamUrl, isRecording)

        return START_STICKY
    }

    private fun acquireLocks() {
        if (wakeLock == null) {
            val pm = getSystemService(PowerManager::class.java)
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ObsWifiCams:StreamingLock")?.apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24 hours max
            }
        }

        if (wifiLock == null) {
            val wm = applicationContext.getSystemService(WifiManager::class.java)
            val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wm?.createWifiLock(lockMode, "ObsWifiCams:WifiStreamLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        wakeLock = null

        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {}
        wifiLock = null
    }

    private fun startForegroundWithNotification(streamUrl: String, isRecording: Boolean) {
        val activityIntent = Intent(this, CameraActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = buildString {
            if (streamUrl.isNotEmpty()) append("Streaming: $streamUrl")
            if (isRecording) {
                if (isNotEmpty()) append(" · ")
                append("Recording active")
            }
        }.ifEmpty { "Background camera service active" }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${getString(R.string.app_name)} Live")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_camera)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        val hasCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var type = 0
            if (hasCamera) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            if (hasAudio) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            type
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
        } catch (e: Exception) {
            Log.e("CameraStreamService", "Failed to start foreground service with type $serviceType, falling back", e)
            try {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && hasCamera) {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
                } else {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
                }
            } catch (e2: Exception) {
                Log.e("CameraStreamService", "Failed to start fallback foreground service", e2)
            }
        }
    }

    private fun stopForegroundService() {
        releaseLocks()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camera Streaming & Recording",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps camera streaming and recording alive when screen is off"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "obs_wifi_stream_service"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.stratatechlabs.obswificams.START_STREAM_SERVICE"
        const val ACTION_STOP = "com.stratatechlabs.obswificams.STOP_STREAM_SERVICE"

        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_IS_RECORDING = "extra_is_recording"

        fun start(context: Context, streamUrl: String = "", isRecording: Boolean = false) {
            val intent = Intent(context, CameraStreamingForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STREAM_URL, streamUrl)
                putExtra(EXTRA_IS_RECORDING, isRecording)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CameraStreamingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
