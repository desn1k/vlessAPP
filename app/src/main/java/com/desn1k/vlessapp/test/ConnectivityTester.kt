package com.desn1k.vlessapp.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * Real-world reachability checks performed against the *currently active* default network
 * (i.e. once XrayVpnService is connected, all of this naturally routes through the tunnel —
 * no proxy-aware HTTP client needed).
 *
 * Android apps can't send raw ICMP without root, so "ping" here means a TCP connect-time
 * probe, which is the standard substitute used by every consumer VPN app.
 */
object ConnectivityTester {

    data class SiteResult(val url: String, val ok: Boolean, val httpCode: Int?, val latencyMs: Long, val error: String? = null)
    data class PingResult(val host: String, val port: Int, val ok: Boolean, val latencyMs: Long, val error: String? = null)

    val DEFAULT_SITES = listOf(
        "https://www.google.com/generate_204",
        "https://www.cloudflare.com/cdn-cgi/trace",
        "https://www.youtube.com",
    )

    val DEFAULT_PING_TARGETS = listOf("1.1.1.1" to 443, "8.8.8.8" to 443)

    suspend fun checkSite(urlString: String, timeoutMs: Int = 8000): SiteResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            val code = connection.responseCode
            val latency = System.currentTimeMillis() - start
            connection.disconnect()
            SiteResult(urlString, code in 200..399, code, latency)
        } catch (t: Throwable) {
            SiteResult(urlString, false, null, System.currentTimeMillis() - start, t.message)
        }
    }

    suspend fun checkSites(urls: List<String> = DEFAULT_SITES): List<SiteResult> =
        urls.map { checkSite(it) }

    suspend fun tcpPing(host: String, port: Int, timeoutMs: Int = 4000): PingResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            PingResult(host, port, true, System.currentTimeMillis() - start)
        } catch (t: Throwable) {
            PingResult(host, port, false, System.currentTimeMillis() - start, t.message)
        }
    }

    suspend fun pingAll(targets: List<Pair<String, Int>> = DEFAULT_PING_TARGETS): List<PingResult> =
        targets.map { (host, port) -> tcpPing(host, port) }
}
