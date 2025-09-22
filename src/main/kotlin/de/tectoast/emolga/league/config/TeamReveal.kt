package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class TeamRevealData(val revealState: MutableMap<Int, Int> = mutableMapOf())