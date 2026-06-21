package com.desn1k.vlessapp.vless

import android.util.Base64
import com.desn1k.vlessapp.data.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches a subscription link, which is conventionally a plain-text or base64-encoded list of
 * vless:// links (one per line), and parses every entry into a Profile.
 */
object SubscriptionImporter {

    suspend fun fetch(url: String): List<Profile> = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            requestMethod = "GET"
        }
        val body = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val text = if (body.contains("vless://")) body else decodeBase64(body) ?: body

        text.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("vless://") }
            .mapNotNull { line -> runCatching { VlessLink.parse(line) }.getOrNull() }
            .map { it.copy(subscriptionUrl = url, tag = it.tag.ifBlank { hostTag(url) }) }
            .toList()
    }

    private fun decodeBase64(text: String): String? = runCatching {
        String(Base64.decode(text.trim(), Base64.DEFAULT))
    }.getOrNull()

    private fun hostTag(url: String): String = runCatching { URL(url).host }.getOrDefault("subscription")
}
