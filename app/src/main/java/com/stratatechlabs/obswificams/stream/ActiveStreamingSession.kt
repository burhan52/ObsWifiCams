package com.stratatechlabs.obswificams.stream

import com.stratatechlabs.obswificams.camera.CameraController
import com.stratatechlabs.obswificams.camera.PersistentCameraLifecycleOwner

/**
 * Holds the active background camera streaming session components so that
 * live-streaming continues seamlessly when CameraActivity is closed,
 * and allows a newly reopened activity or the foreground service to attach to
 * or release the active session.
 */
data class ActiveStreamingSession(
    val cameraLifecycleOwner: PersistentCameraLifecycleOwner,
    val controller: CameraController,
    val wifiStreamManager: WifiStreamManager,
) {
    companion object {
        @Volatile
        var active: ActiveStreamingSession? = null
    }
}
