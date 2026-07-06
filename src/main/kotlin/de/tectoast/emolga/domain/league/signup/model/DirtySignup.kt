package de.tectoast.emolga.domain.league.signup.model

data class DirtySignup(val id: Int, val config: LeagueSignupConfig, val guild: Long, val announceMessageId: Long, val userCount: Long)