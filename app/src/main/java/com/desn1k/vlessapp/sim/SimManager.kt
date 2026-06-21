package com.desn1k.vlessapp.sim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

/**
 * Enumerates the SIMs currently active on the device (dual-SIM included) using
 * SubscriptionManager. Requires READ_PHONE_STATE at runtime (Android 6+).
 */
object SimManager {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun listActiveSims(context: Context): List<SimInfo> {
        if (!hasPermission(context)) return emptyList()
        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                ?: return emptyList()

        return try {
            val infos = subscriptionManager.activeSubscriptionInfoList.orEmpty()
            infos.map { info ->
                SimInfo(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    carrierName = info.carrierName?.toString().orEmpty().ifBlank { "Unknown" },
                    displayName = info.displayName?.toString().orEmpty(),
                    countryIso = info.countryIso.orEmpty(),
                    isActive = true
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
