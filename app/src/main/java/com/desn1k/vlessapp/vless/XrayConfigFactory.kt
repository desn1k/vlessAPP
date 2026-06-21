package com.desn1k.vlessapp.vless

import com.desn1k.vlessapp.data.Profile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds Xray-core JSON configs (the format consumed by libv2ray.CoreController.StartLoop).
 *
 * Two flavors are produced:
 *  - [forTunnel] : full config with a local SOCKS/HTTP inbound plus, when [withTun] is set, a
 *    "tun" inbound so XrayVpnService can hand it the VPN file descriptor.
 *  - [forProbe]  : inbound-less config used only to measure outbound delay (see
 *    libv2ray's MeasureOutboundDelay), so a quick "does this server work" check never needs
 *    VPN permission at all.
 */
object XrayConfigFactory {

    private const val SOCKS_PORT = 10808
    private const val HTTP_PORT = 10809

    fun forProbe(profile: Profile): String = base(profile, inbounds = JSONArray()).toString()

    fun forTunnel(profile: Profile, withTun: Boolean, mtu: Int = 1500): String {
        val inbounds = JSONArray().apply {
            put(socksInbound())
            put(httpInbound())
            if (withTun) put(tunInbound(mtu))
        }
        return base(profile, inbounds).toString()
    }

    private fun socksInbound() = JSONObject().apply {
        put("tag", "socks")
        put("port", SOCKS_PORT)
        put("listen", "127.0.0.1")
        put("protocol", "socks")
        put("settings", JSONObject().apply {
            put("auth", "noauth")
            put("udp", true)
        })
        put("sniffing", sniffing())
    }

    private fun httpInbound() = JSONObject().apply {
        put("tag", "http")
        put("port", HTTP_PORT)
        put("listen", "127.0.0.1")
        put("protocol", "http")
    }

    private fun tunInbound(mtu: Int) = JSONObject().apply {
        put("tag", "tun")
        put("protocol", "tun")
        put("settings", JSONObject().apply {
            put("name", "xray0")
            put("MTU", mtu)
        })
        put("sniffing", sniffing())
    }

    private fun sniffing() = JSONObject().apply {
        put("enabled", true)
        put("destOverride", JSONArray(listOf("http", "tls", "quic")))
    }

    private fun base(profile: Profile, inbounds: JSONArray): JSONObject = JSONObject().apply {
        put("log", JSONObject().apply { put("loglevel", "warning") })
        put("inbounds", inbounds)
        put("outbounds", JSONArray().apply {
            put(vlessOutbound(profile))
            put(JSONObject().apply {
                put("tag", "direct")
                put("protocol", "freedom")
            })
            put(JSONObject().apply {
                put("tag", "block")
                put("protocol", "blackhole")
            })
        })
        put("routing", JSONObject().apply {
            put("domainStrategy", "AsIs")
            put("rules", JSONArray().apply {
                if (inbounds.length() > 0) {
                    put(JSONObject().apply {
                        put("type", "field")
                        put("inboundTag", JSONArray(listOf("tun")))
                        put("port", "53")
                        put("outboundTag", "proxy")
                    })
                }
            })
        })
    }

    private fun vlessOutbound(profile: Profile): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "vless")
        put("settings", JSONObject().apply {
            put("vnext", JSONArray().apply {
                put(JSONObject().apply {
                    put("address", profile.address)
                    put("port", profile.port)
                    put("users", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", profile.uuid)
                            put("encryption", profile.encryption.ifBlank { "none" })
                            if (profile.flow.isNotBlank()) put("flow", profile.flow)
                            put("level", 8)
                        })
                    })
                })
            })
        })
        put("streamSettings", streamSettings(profile))
        put("mux", JSONObject().apply { put("enabled", false) })
    }

    private fun streamSettings(profile: Profile): JSONObject = JSONObject().apply {
        put("network", profile.network.ifBlank { "tcp" })
        put("security", profile.security.ifBlank { "none" })

        when (profile.security) {
            "tls" -> put("tlsSettings", JSONObject().apply {
                if (profile.sni.isNotBlank()) put("serverName", profile.sni)
                put("allowInsecure", profile.allowInsecure)
                if (profile.fingerprint.isNotBlank()) put("fingerprint", profile.fingerprint)
                if (profile.alpn.isNotBlank()) {
                    put("alpn", JSONArray(profile.alpn.split(",").map { it.trim() }))
                }
            })
            "reality" -> put("realitySettings", JSONObject().apply {
                if (profile.sni.isNotBlank()) put("serverName", profile.sni)
                if (profile.fingerprint.isNotBlank()) put("fingerprint", profile.fingerprint)
                put("publicKey", profile.publicKey)
                if (profile.shortId.isNotBlank()) put("shortId", profile.shortId)
                if (profile.spiderX.isNotBlank()) put("spiderX", profile.spiderX)
            })
        }

        when (profile.network) {
            "ws" -> put("wsSettings", JSONObject().apply {
                put("path", profile.wsPath.ifBlank { "/" })
                if (profile.wsHost.isNotBlank()) {
                    put("headers", JSONObject().apply { put("Host", profile.wsHost) })
                }
            })
            "grpc" -> put("grpcSettings", JSONObject().apply {
                put("serviceName", profile.grpcServiceName)
                put("multiMode", false)
            })
        }
    }
}
