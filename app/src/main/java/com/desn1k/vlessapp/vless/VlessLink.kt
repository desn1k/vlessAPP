package com.desn1k.vlessapp.vless

import com.desn1k.vlessapp.data.Profile
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Parses/serializes the de-facto "vless://" share link format used by Xray/V2Ray clients:
 *
 *   vless://<uuid>@<host>:<port>?encryption=none&security=reality&sni=...&fp=...
 *           &pbk=...&sid=...&spx=...&flow=...&type=ws&path=...&host=...&serviceName=...#remark
 */
object VlessLink {

    fun parse(rawLink: String): Profile {
        val link = rawLink.trim()
        require(link.startsWith("vless://")) { "Not a vless:// link" }

        val uri = URI(link)
        val uuid = uri.userInfo ?: error("Missing UUID in vless link")
        val host = uri.host ?: error("Missing host in vless link")
        val port = if (uri.port != -1) uri.port else 443

        val params = parseQuery(uri.rawQuery.orEmpty())
        val remark = uri.rawFragment?.let { decode(it) }?.takeIf { it.isNotBlank() } ?: "$host:$port"

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
            val key = decode(pair.substring(0, idx))
            val value = decode(pair.substring(idx + 1))
            key to value
        }.toMap()
    }

    private fun decode(value: String) = URLDecoder.decode(value, "UTF-8")
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
