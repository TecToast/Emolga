package de.tectoast.emolga.domain.league.teamgraphic.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable


@Serializable
data class PokemonCropData(val showdownId: ShowdownID, val x: Int, val y: Int, val size: Int, val flipped: Boolean)