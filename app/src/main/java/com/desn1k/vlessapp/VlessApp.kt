package com.desn1k.vlessapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.desn1k.vlessapp.data.AppDatabase

class VlessApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            VPN_NOTIFICATION_CHANNEL_ID,
            "Vless Checker VPN",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val VPN_NOTIFICATION_CHANNEL_ID = "vless_vpn_channel"
    }
}
