package de.tectoast.emolga.domain.league.gamedata.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class LeagueEvent(
    val leagueName: String,
    val week: Int,
    val matchNum: Int,
    val timestamp: Instant,
    val uindices: List<Int>,
    val specificData: LeagueEventSpecificData
)