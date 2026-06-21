package com.desn1k.vlessapp.sim

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val carrierName: String,
    val displayName: String,
    val countryIso: String,
    val isActive: Boolean
)
