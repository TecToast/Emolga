package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class HideGamesConfig(
    val gamedays: Set<Int>,
    val replayChannel: Long,
    val resultChannel: Long,
)