package com.desn1k.vlessapp.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide observable VPN status, updated by XrayVpnService and read by the UI.
 */
object ConnectionState {

    enum class Status { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    data class State(val status: Status, val detail: String? = null)

    private val _state = MutableStateFlow(State(Status.DISCONNECTED))
    val state: StateFlow<State> = _state

    fun update(status: Status, detail: String?) {
        _state.value = State(status, detail)
    }
}
