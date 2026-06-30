package de.tectoast.emolga.domain.guildspecific.laddertournament.model

import kotlinx.serialization.Serializable

@Serializable
data class LadderTournamentConfig(
    val adminChannel: Long,
    val signupChannel: Long,
    val formats: Map<String, String>,
    val sid: String,
    val cols: List<LadderTournamentCol>,
    val sortCols: List<LadderTournamentCol>,
    val lastExecution: Long,
    val durationInHours: Int,
    val amount: Int,
    val sdNamePrefix: String,
)