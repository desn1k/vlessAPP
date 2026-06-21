package com.desn1k.vlessapp.sim

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TelephonyNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Obtains the actual cellular [Network] object behind a given subscription, so traffic
 * can be explicitly routed over one SIM regardless of which one Android currently treats
 * as "default" (needed to test every operator independently of the active VPN/SIM).
 */
object CellularNetworkBinder {

    suspend fun requestNetworkForSubscription(
        context: Context,
        subscriptionId: Int,
        timeoutMs: Long = 8000
    ): Network? = suspendCancellableCoroutine { cont ->
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val requestBuilder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBuilder.setNetworkSpecifier(
                TelephonyNetworkSpecifier.Builder()
                    .setSubscriptionId(subscriptionId)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            requestBuilder.setNetworkSpecifier(subscriptionId.toString())
        }

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
            connectivityManager.requestNetwork(requestBuilder.build(), callback, timeoutMs.toInt())
        } catch (t: Throwable) {
            if (cont.isActive) cont.resume(null)
        }
    }
}
