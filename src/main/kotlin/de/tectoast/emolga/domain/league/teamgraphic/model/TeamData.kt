package de.tectoast.emolga.domain.league.teamgraphic.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import java.awt.image.BufferedImage

data class TeamData(
    val teamOwner: String?,
    val teamName: String?,
    val logo: BufferedImage?,
    val picks: Map<Int, ShowdownID>,
    val leaguename: String,
    val idx: Int,
    val users: List<Long>
)