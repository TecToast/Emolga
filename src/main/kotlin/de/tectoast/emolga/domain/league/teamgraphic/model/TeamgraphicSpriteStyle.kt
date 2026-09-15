package de.tectoast.emolga.domain.league.teamgraphic.model

import kotlinx.serialization.Serializable

@Serializable
enum class TeamgraphicSpriteStyle(val nearestNeighborInterpolation: Boolean) {
    SUGIMORI(false), HOME(false), GEN5(true)
}
