package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class AllowPickDuringSwitchConfig(
    val enabled: Boolean = false
)