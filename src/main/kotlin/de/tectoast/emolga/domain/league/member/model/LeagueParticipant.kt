package de.tectoast.emolga.domain.league.member.model

import kotlinx.serialization.Serializable

@Serializable
data class LeagueParticipant(val idx: Int, val userId: Long, val substitute: Boolean, val shouldPing: Boolean)