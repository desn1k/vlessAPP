package com.desn1k.vlessapp.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * A single VLESS server profile. Fields mirror the parameters carried by a
 * vless:// share link (see VlessLink.parse / VlessLink.toUri).
 */
@Parcelize
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remark: String,
    val address: String,
    val port: Int,
    val uuid: String,
    val flow: String = "",
    val encryption: String = "none",
    val network: String = "tcp",            // tcp | ws | grpc
    val security: String = "none",          // none | tls | reality
    val sni: String = "",
    val fingerprint: String = "chrome",
    val alpn: String = "",
    val allowInsecure: Boolean = false,
    val publicKey: String = "",             // reality pbk
    val shortId: String = "",               // reality sid
    val spiderX: String = "",               // reality spx
    val wsPath: String = "",
    val wsHost: String = "",
    val grpcServiceName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLatencyMs: Long = -1,
    val lastCheckedAt: Long = 0,
    val tag: String = "",                   // user-defined group, e.g. country or subscription name
    val subscriptionUrl: String = ""        // set when imported from a subscription link
) : Parcelable
