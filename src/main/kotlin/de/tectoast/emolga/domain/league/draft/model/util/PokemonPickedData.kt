package de.tectoast.emolga.domain.league.draft.model.util

import kotlinx.serialization.Serializable

@Serializable
data class PokemonPickedData(
    val name: String, val tier: String, val divs: List<DivisionPickedData>, val spriteName: String, val amount: Int
)