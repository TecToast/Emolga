package de.tectoast.emolga.domain.league.gamedata.model

import de.tectoast.emolga.domain.game.model.KDWithName
import kotlinx.serialization.Serializable

@Serializable
data class GameData(
    val kd: List<List<KDWithName>>, val winnerIndex: Int, val url: String
)
