package de.tectoast.emolga.domain.league.gamedata.model

import kotlin.time.Instant

data class LeagueEvent(
    val leagueName: String,
    val week: Int,
    val matchNum: Int,
    val timestamp: Instant,
    val uindices: List<Int>,
    val specificData: LeagueEventSpecificData
)