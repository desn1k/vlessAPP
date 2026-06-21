package com.desn1k.vlessapp.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.desn1k.vlessapp.MainActivity
import com.desn1k.vlessapp.VlessApp
import com.desn1k.vlessapp.core.CoreCallback
import com.desn1k.vlessapp.core.CoreManager
import com.desn1k.vlessapp.data.Profile
import com.desn1k.vlessapp.vless.XrayConfigFactory

/**
 * Establishes a system-wide TUN interface and hands its file descriptor straight to
 * Xray-core's native "tun" inbound (gVisor-based netstack inside libv2ray) — no separate
 * tun2socks process needed, matching current v2rayNG behaviour.
 */
class XrayVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> {
                val profile = intent?.getParcelableExtraCompat()
                if (profile == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification())
                startVpn(profile)
            }
        }
        return START_STICKY
    }

    private fun startVpn(profile: Profile) {
        val builder = Builder()
            .setSession(profile.remark)
            .setMtu(MTU)
            .addAddress(VPN_ADDRESS_V4, 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(DNS_PRIMARY)
            .addDnsServer(DNS_SECONDARY)
            .addDisallowedApplication(packageName)

        val pfd = try {
            builder.establish()
        } catch (t: Throwable) {
            ConnectionState.update(ConnectionState.Status.ERROR, t.message ?: "VPN setup failed")
            stopSelf()
            return
        }

        if (pfd == null) {
            ConnectionState.update(ConnectionState.Status.ERROR, "VPN permission not granted")
            stopSelf()
            return
        }
        tunInterface = pfd

        val configJson = XrayConfigFactory.forTunnel(profile, withTun = true, mtu = MTU)
        val callback = CoreCallback(
            onStatus = { code, message ->
                ConnectionState.update(
                    if (code.toInt() == 0 && CoreManager.isRunning()) ConnectionState.Status.CONNECTED
                    else ConnectionState.Status.ERROR,
                    message
                )
                0L
            }
        )

        val started = CoreManager.start(this, configJson, pfd.fd, callback)
        if (!started) {
            ConnectionState.update(ConnectionState.Status.ERROR, "Xray core failed to start")
            stopVpn()
            return
        }
        ConnectionState.update(ConnectionState.Status.CONNECTED, profile.remark)
    }

    private fun stopVpn() {
        CoreManager.stop()
        try {
            tunInterface?.close()
        } catch (_: Throwable) {
        }
        tunInterface = null
        ConnectionState.update(ConnectionState.Status.DISCONNECTED, null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, VlessApp.VPN_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("VLESS VPN")
            .setContentText("Tunnel active")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    private fun Intent.getParcelableExtraCompat(): Profile? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(EXTRA_PROFILE, Profile::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(EXTRA_PROFILE)
        }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val MTU = 1500
        private const val VPN_ADDRESS_V4 = "10.10.10.1"
        private const val DNS_PRIMARY = "1.1.1.1"
        private const val DNS_SECONDARY = "8.8.8.8"
        const val ACTION_STOP = "com.desn1k.vlessapp.action.STOP"
        const val EXTRA_PROFILE = "profile"

        fun start(context: Context, profile: Profile) {
            val intent = Intent(context, XrayVpnService::class.java).putExtra(EXTRA_PROFILE, profile)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, XrayVpnService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
