package de.tectoast.emolga.domain.league.teamgraphic.model

import kotlinx.serialization.Serializable

@Serializable
data class PokemonCropOverview(val spriteStyle: TeamgraphicSpriteStyle, val data: List<PokemonCropInfoData>)