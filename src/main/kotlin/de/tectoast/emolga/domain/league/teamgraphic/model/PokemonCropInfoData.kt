package de.tectoast.emolga.domain.league.teamgraphic.model

import kotlinx.serialization.Serializable


@Serializable
data class PokemonCropInfoData(
    val spriteName: String,
    val official: String,
    val x: Int,
    val y: Int,
    val size: Int,
    val flipped: Boolean
)
