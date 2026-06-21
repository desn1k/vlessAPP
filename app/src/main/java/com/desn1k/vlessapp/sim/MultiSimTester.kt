package com.desn1k.vlessapp.sim

import android.content.Context
import com.desn1k.vlessapp.data.Profile
import com.desn1k.vlessapp.test.ConnectivityTester

data class OperatorTestResult(
    val sim: SimInfo,
    val networkAcquired: Boolean,
    val serverPing: ConnectivityTester.PingResult? = null,
    val sites: List<ConnectivityTester.SiteResult> = emptyList(),
    val pings: List<ConnectivityTester.PingResult> = emptyList()
)

/**
 * Runs the same reachability checks used elsewhere in the app once per active SIM, each
 * pinned to that SIM's own cellular network via CellularNetworkBinder — so you can see,
 * operator by operator, whether the VLESS server (and the internet in general) is reachable.
 */
object MultiSimTester {

    suspend fun runForAllSims(context: Context, profile: Profile?): List<OperatorTestResult> {
        val sims = SimManager.listActiveSims(context)
        return sims.map { sim -> runForSim(context, sim, profile) }
    }

    suspend fun runForSim(context: Context, sim: SimInfo, profile: Profile?): OperatorTestResult {
        val network = CellularNetworkBinder.requestNetworkForSubscription(context, sim.subscriptionId)
        if (network == null) {
            return OperatorTestResult(sim = sim, networkAcquired = false)
        }

        val serverPing = profile?.let {
            ConnectivityTester.tcpPing(it.address, it.port, network)
        }
        val sites = ConnectivityTester.checkSites(network = network)
        val pings = ConnectivityTester.pingAll(network = network)

        return OperatorTestResult(
            sim = sim,
            networkAcquired = true,
            serverPing = serverPing,
            sites = sites,
            pings = pings
        )
    }
}
