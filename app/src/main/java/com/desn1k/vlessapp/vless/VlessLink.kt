package com.desn1k.vlessapp.vless

import com.desn1k.vlessapp.data.Profile
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Parses/serializes the de-facto "vless://" share link format used by Xray/V2Ray clients:
 *
 *   vless://<uuid>@<host>:<port>?encryption=none&security=reality&sni=...&fp=...
 *           &pbk=...&sid=...&spx=...&flow=...&type=ws&path=...&host=...&serviceName=...#remark
 *
 * Parsed by hand (not via java.net.URI) because real-world share links commonly put raw,
 * non-percent-encoded Unicode (Cyrillic remarks, flag emoji) in the fragment, which java.net.URI
 * rejects with a URISyntaxException.
 */
object VlessLink {

    fun parse(rawLink: String): Profile {
        val link = rawLink.trim()
        require(link.startsWith("vless://", ignoreCase = true)) { "Not a vless:// link" }

        var rest = link.substring("vless://".length)

        val rawRemark = rest.substringAfter('#', "")
        rest = rest.substringBefore('#')

        val rawQuery = rest.substringAfter('?', "")
        rest = rest.substringBefore('?')

        val atIdx = rest.indexOf('@')
        require(atIdx >= 0) { "Missing UUID in vless link" }
        val uuid = rest.substring(0, atIdx).takeIf { it.isNotBlank() } ?: error("Missing UUID in vless link")
        val hostPort = rest.substring(atIdx + 1)
        require(hostPort.isNotBlank()) { "Missing host in vless link" }

        val (host, port) = parseHostPort(hostPort)

        val params = parseQuery(rawQuery)
        val remark = decodeLoose(rawRemark).takeIf { it.isNotBlank() } ?: "$host:$port"

        return Profile(
            remark = remark,
            address = host,
            port = port,
            uuid = uuid,
            flow = params["flow"].orEmpty(),
            encryption = params["encryption"] ?: "none",
            network = params["type"] ?: "tcp",
            security = params["security"] ?: "none",
            sni = params["sni"] ?: params["peer"].orEmpty(),
            fingerprint = params["fp"] ?: "chrome",
            alpn = params["alpn"].orEmpty(),
            allowInsecure = params["allowInsecure"] == "1" || params["allowInsecure"] == "true",
            publicKey = params["pbk"].orEmpty(),
            shortId = params["sid"].orEmpty(),
            spiderX = params["spx"].orEmpty(),
            wsPath = params["path"].orEmpty(),
            wsHost = params["host"].orEmpty(),
            grpcServiceName = params["serviceName"].orEmpty()
        )
    }

    /** Splits "host:port" (or "[ipv6]:port") without validating against RFC 3986. */
    private fun parseHostPort(hostPort: String): Pair<String, Int> {
        val (host, portPart) = if (hostPort.startsWith("[")) {
            val end = hostPort.indexOf(']')
            require(end > 1) { "Malformed IPv6 host in vless link" }
            val h = hostPort.substring(1, end)
            val p = hostPort.substring(end + 1).removePrefix(":")
            h to p
        } else {
            val idx = hostPort.lastIndexOf(':')
            if (idx >= 0) hostPort.substring(0, idx) to hostPort.substring(idx + 1) else hostPort to ""
        }
        require(host.isNotBlank()) { "Missing host in vless link" }
        val port = if (portPart.isBlank()) 443 else portPart.toIntOrNull() ?: error("Invalid port in vless link: $portPart")
        return host to port
    }

    fun toLink(profile: Profile): String {
        val params = buildMap {
            put("encryption", profile.encryption)
            put("type", profile.network)
            put("security", profile.security)
            if (profile.flow.isNotBlank()) put("flow", profile.flow)
            if (profile.sni.isNotBlank()) put("sni", profile.sni)
            if (profile.fingerprint.isNotBlank()) put("fp", profile.fingerprint)
            if (profile.alpn.isNotBlank()) put("alpn", profile.alpn)
            if (profile.allowInsecure) put("allowInsecure", "1")
            if (profile.publicKey.isNotBlank()) put("pbk", profile.publicKey)
            if (profile.shortId.isNotBlank()) put("sid", profile.shortId)
            if (profile.spiderX.isNotBlank()) put("spx", profile.spiderX)
            if (profile.wsPath.isNotBlank()) put("path", profile.wsPath)
            if (profile.wsHost.isNotBlank()) put("host", profile.wsHost)
            if (profile.grpcServiceName.isNotBlank()) put("serviceName", profile.grpcServiceName)
        }
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${encode(v)}" }
        val fragment = encode(profile.remark)
        return "vless://${profile.uuid}@${profile.address}:${profile.port}?$query#$fragment"
    }

    private fun parseQuery(rawQuery: String): Map<String, String> {
        if (rawQuery.isBlank()) return emptyMap()
        return rawQuery.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) return@mapNotNull null
            val key = decodeLoose(pair.substring(0, idx))
            val value = decodeLoose(pair.substring(idx + 1))
            key to value
        }.toMap()
    }

    /** Percent-decodes if possible, otherwise returns the input as-is (raw Unicode is common in remarks). */
    private fun decodeLoose(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
