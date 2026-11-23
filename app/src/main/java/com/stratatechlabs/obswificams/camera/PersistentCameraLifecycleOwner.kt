package com.stratatechlabs.obswificams.camera

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * A [LifecycleOwner] for CameraX that mirrors [parent] (e.g. CameraActivity),
 * but remains in [Lifecycle.State.RESUMED] during sleep, screen lock, or background
 * as long as [keepAlive] returns true (i.e. Wi-Fi camera streaming or recording is active).
 */
class PersistentCameraLifecycleOwner(
    parent: LifecycleOwner?,
    private val keepAlive: () -> Boolean,
) : LifecycleOwner {

    override val lifecycle: LifecycleRegistry = LifecycleRegistry(this)

    private var currentParent: LifecycleOwner? = parent

    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            Lifecycle.Event.ON_START -> lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
            Lifecycle.Event.ON_RESUME -> lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            Lifecycle.Event.ON_PAUSE -> {
                if (!keepAlive()) {
                    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                }
            }
            Lifecycle.Event.ON_STOP -> {
                if (!keepAlive()) {
                    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                detachParent()
                if (!keepAlive()) {
                    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                }
            }
            Lifecycle.Event.ON_ANY -> Unit
        }
    }

    init {
        currentParent?.lifecycle?.addObserver(observer)
    }

    fun attachParent(newParent: LifecycleOwner) {
        if (currentParent === newParent) return
        detachParent()
        currentParent = newParent
        newParent.lifecycle.addObserver(observer)
        onBackgroundStateChanged()
    }

    fun detachParent() {
        currentParent?.lifecycle?.removeObserver(observer)
        currentParent = null
    }

    /**
     * Called whenever streaming or recording state changes.
     * Keeps camera active in background when streaming/recording is active,
     * or moves camera to match parent state when background work has concluded.
     */
    fun onBackgroundStateChanged() {
        val parentState = currentParent?.lifecycle?.currentState
        if (!keepAlive()) {
            if (parentState == null || parentState == Lifecycle.State.DESTROYED) {
                if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                }
            } else if ((parentState == Lifecycle.State.CREATED) && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            } else if ((parentState == Lifecycle.State.STARTED) && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }
        } else {
            if (lifecycle.currentState == Lifecycle.State.CREATED) {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        }
    }

    fun release() {
        detachParent()
        if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }
}
