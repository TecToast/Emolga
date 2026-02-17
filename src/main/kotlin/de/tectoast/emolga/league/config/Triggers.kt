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
    // TODO remove
    val updraftDisabled: Boolean = false,
    val randomBattle: Boolean = false,
)