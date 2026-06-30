package de.tectoast.emolga.domain.league.core.model

data class LeagueWithParticipants(val leagueName: String, val guild: Long, val users: List<Long>)

