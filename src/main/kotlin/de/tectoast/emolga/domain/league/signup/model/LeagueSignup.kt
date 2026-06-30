package de.tectoast.emolga.domain.league.signup.model

data class LeagueSignup(
    val id: Int,
    val guild: Long,
    val identifier: String,
    val config: LeagueSignupConfig,
    val announceMessageId: Long? = null,
    val conferences: List<String> = emptyList(),
    val conferenceRoleIds: Map<String, Long> = emptyMap()
)