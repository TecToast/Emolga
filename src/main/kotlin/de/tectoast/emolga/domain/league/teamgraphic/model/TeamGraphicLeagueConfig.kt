package de.tectoast.emolga.domain.league.teamgraphic.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamGraphicLeagueConfig(
    val style: TeamGraphicStyle
)
