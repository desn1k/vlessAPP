package com.desn1k.vlessapp.data

import org.json.JSONArray
import org.json.JSONObject

/** Serializes/deserializes profiles to a plain JSON array, used for the export/import backup feature. */
object ProfileBackup {

    fun toJson(profiles: List<Profile>): String {
        val array = JSONArray()
        profiles.forEach { p ->
            array.put(
                JSONObject().apply {
                    put("remark", p.remark)
                    put("address", p.address)
                    put("port", p.port)
                    put("uuid", p.uuid)
                    put("flow", p.flow)
                    put("encryption", p.encryption)
                    put("network", p.network)
                    put("security", p.security)
                    put("sni", p.sni)
                    put("fingerprint", p.fingerprint)
                    put("alpn", p.alpn)
                    put("allowInsecure", p.allowInsecure)
                    put("publicKey", p.publicKey)
                    put("shortId", p.shortId)
                    put("spiderX", p.spiderX)
                    put("wsPath", p.wsPath)
                    put("wsHost", p.wsHost)
                    put("grpcServiceName", p.grpcServiceName)
                    put("tag", p.tag)
                    put("subscriptionUrl", p.subscriptionUrl)
                }
            )
        }
        return array.toString(2)
    }

    fun fromJson(json: String): List<Profile> {
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Profile(
                        remark = o.optString("remark"),
                        address = o.optString("address"),
                        port = o.optInt("port", 443),
                        uuid = o.optString("uuid"),
                        flow = o.optString("flow"),
                        encryption = o.optString("encryption", "none"),
                        network = o.optString("network", "tcp"),
                        security = o.optString("security", "none"),
                        sni = o.optString("sni"),
                        fingerprint = o.optString("fingerprint", "chrome"),
                        alpn = o.optString("alpn"),
                        allowInsecure = o.optBoolean("allowInsecure", false),
                        publicKey = o.optString("publicKey"),
                        shortId = o.optString("shortId"),
                        spiderX = o.optString("spiderX"),
                        wsPath = o.optString("wsPath"),
                        wsHost = o.optString("wsHost"),
                        grpcServiceName = o.optString("grpcServiceName"),
                        tag = o.optString("tag"),
                        subscriptionUrl = o.optString("subscriptionUrl")
                    )
                )
            }
        }
    }
}
