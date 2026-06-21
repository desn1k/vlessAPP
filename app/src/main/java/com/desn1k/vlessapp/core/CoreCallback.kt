package com.desn1k.vlessapp.core

import libv2ray.CoreCallbackHandler

/**
 * Bridges Xray-core lifecycle events (Go side) back to Kotlin.
 *
 * gomobile maps Go's plain `int` to Java `long` and lowercases exported method
 * names, hence the (Long) signatures below even though the Go interface uses `int`.
 */
open class CoreCallback(
    private val onStartup: () -> Long = { 0L },
    private val onShutdown: () -> Long = { 0L },
    private val onStatus: (Long, String?) -> Long = { _, _ -> 0L }
) : CoreCallbackHandler {
    override fun startup(): Long = onStartup()
    override fun shutdown(): Long = onShutdown()
    override fun onEmitStatus(code: Long, message: String?): Long = onStatus(code, message)
}
