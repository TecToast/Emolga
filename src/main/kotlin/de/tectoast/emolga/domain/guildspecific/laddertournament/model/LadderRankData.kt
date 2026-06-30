package de.tectoast.emolga.domain.guildspecific.laddertournament.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LadderRankData(
    @SerialName("w") val wins: Int = 0,
    @SerialName("l") val losses: Int = 0,
    @SerialName("t") val ties: Int = 0,
    val gxe: Double,
    val elo: Double
)
