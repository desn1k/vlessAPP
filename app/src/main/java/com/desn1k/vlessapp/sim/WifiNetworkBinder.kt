package com.desn1k.vlessapp.sim

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Mirrors CellularNetworkBinder but for the active Wi-Fi network, so connectivity tests can be
 * pinned explicitly to Wi-Fi regardless of which network Android currently treats as default
 * (e.g. while the VPN tunnel is up).
 */
object WifiNetworkBinder {

    suspend fun requestWifiNetwork(context: Context, timeoutMs: Long = 8000): Network? =
        suspendCancellableCoroutine { cont ->
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    connectivityManager.unregisterNetworkCallback(this)
                    if (cont.isActive) cont.resume(network)
                }

                override fun onUnavailable() {
                    if (cont.isActive) cont.resume(null)
                }
            }

            cont.invokeOnCancellation {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (_: Throwable) {
                }
            }

            try {
                connectivityManager.requestNetwork(request, callback, timeoutMs.toInt())
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(null)
            }
        }
}
