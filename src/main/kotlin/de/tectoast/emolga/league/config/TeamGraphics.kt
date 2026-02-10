package de.tectoast.emolga.league.config

import de.tectoast.emolga.utils.teamgraphics.TeamGraphicStyle
import kotlinx.serialization.Serializable

@Serializable
data class TeamGraphicsLeagueConfig(
    val style: TeamGraphicStyle,
    val channel: Long? = null
)