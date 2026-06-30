package de.tectoast.emolga.domain.league.signup.model

data class DirtySignup(val config: LeagueSignupConfig, val guild: Long, val announceMessageId: Long, val userCount: Int)