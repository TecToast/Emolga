package de.tectoast.emolga.domain.league.draft.model.core

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class DraftPokemon(
    var showdownId: ShowdownID,
    var tier: String,
    var free: Boolean = false,
    var quit: Boolean = false,
    var noCost: Boolean = false,
    var tera: Boolean = false,
)
