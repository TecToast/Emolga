package de.tectoast.emolga.domain.league.admin.model

import de.tectoast.emolga.domain.league.teamgraphic.model.TeamgraphicShape
import kotlinx.serialization.Serializable

@Serializable
data class GuildMeta(
    val id: String,
    val name: String,
    val icon: String,
    val runningSignup: Boolean,
    val teamgraphicShape: TeamgraphicShape? = null
)
