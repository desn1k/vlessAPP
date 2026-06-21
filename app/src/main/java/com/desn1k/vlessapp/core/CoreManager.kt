package com.desn1k.vlessapp.core

import android.content.Context
import android.util.Log
import go.Seq
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Thin wrapper around the gomobile-bound Xray core (libv2ray.aar, built from
 * https://github.com/2dust/AndroidLibXrayLite — see scripts/build_libv2ray.sh).
 *
 * Singleton: only one Xray instance may run in this process at a time, which matches
 * how the underlying CoreController behaves.
 */
object CoreManager {

    private const val TAG = "CoreManager"

    private var controller: CoreController? = null
    private var initialized = false

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return
        Seq.setContext(context.applicationContext)
        // Xray needs a writable dir for geoip/geosite assets; we ship none, so this just
        // points InitCoreEnv at the app's files dir to avoid touching read-only storage.
        Libv2ray.initCoreEnv(context.filesDir.absolutePath, "")
        initialized = true
    }

    @Synchronized
    fun start(context: Context, configJson: String, tunFd: Int, callback: CoreCallback): Boolean {
        ensureInitialized(context)
        stop()
        val ctrl = Libv2ray.newCoreController(callback)
        controller = ctrl
        return try {
            ctrl.startLoop(configJson, tunFd)
            ctrl.isRunning
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start Xray core", t)
            false
        }
    }

    @Synchronized
    fun stop() {
        controller?.let {
            try {
                it.stopLoop()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to stop Xray core", t)
            }
        }
        controller = null
    }

    @Synchronized
    fun isRunning(): Boolean = controller?.isRunning ?: false

    /**
     * One-off latency probe against [configJson] without starting any local inbound or
     * touching VpnService at all (uses Xray's own MeasureOutboundDelay). Used for the
     * "test this server" button before the user grants VPN permission.
     */
    fun probeDelay(context: Context, configJson: String, url: String): Long {
        ensureInitialized(context)
        return try {
            Libv2ray.measureOutboundDelay(configJson, url)
        } catch (t: Throwable) {
            Log.e(TAG, "probeDelay failed", t)
            -1
        }
    }
}
