package de.tectoast.emolga.domain.league.result.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class ResultCodePokemon(val tlName: String, val spriteName: String, val showdownId: ShowdownID)
