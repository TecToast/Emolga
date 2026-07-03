package de.tectoast.emolga.domain.league.gamedata.model

import kotlinx.serialization.Serializable

@Serializable
data class FullGameData(
    val uindices: List<Int>,
    val week: Int,
    val battleIndex: Int,
    val games: List<GameData>,
) {
    private val groupedByWinnerIndex by lazy {
        games.groupBy { it.winnerIndex }
    }
    private val winnerIndex by lazy {
        groupedByWinnerIndex.maxBy { it.value.size }.key
    }
    val winnerIdx by lazy {
        uindices[winnerIndex]
    }
}

