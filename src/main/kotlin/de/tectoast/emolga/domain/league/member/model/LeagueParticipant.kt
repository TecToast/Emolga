package de.tectoast.emolga.domain.league.member.model

data class LeagueParticipant(val idx: Int, val userId: Long, val substitute: Boolean, val shouldPing: Boolean)