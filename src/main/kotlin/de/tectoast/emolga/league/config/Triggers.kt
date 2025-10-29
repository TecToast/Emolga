package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class Triggers(
    val queuePicks: Boolean = true,
    val allowPickDuringSwitch: Boolean = false,
    val alwaysSendTierOnPick: Boolean = false,
    val saveReplayData: Boolean = false,
    val bo3: Boolean = false,
    val teamSubmit: Boolean = false,
    val updraftDisabled: Boolean = false,
)