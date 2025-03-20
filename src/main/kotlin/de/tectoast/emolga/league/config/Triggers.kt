package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class Triggers(
    val queuePicks: Boolean = true,
    val allowPickDuringSwitch: Boolean = false,
    val alwaysSendTierOnPick: Boolean = false,
    val teraPick: Boolean = false,
)