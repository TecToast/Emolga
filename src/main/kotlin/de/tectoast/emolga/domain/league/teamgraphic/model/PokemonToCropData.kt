package de.tectoast.emolga.domain.league.teamgraphic.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class PokemonToCropData(
    val displayName: String,
    val official: ShowdownID,
    val path: String,
    val done: Long,
    val total: Long
)