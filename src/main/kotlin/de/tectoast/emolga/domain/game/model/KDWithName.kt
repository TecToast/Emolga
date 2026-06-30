package de.tectoast.emolga.domain.game.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class KDWithName(val name: ShowdownID, val kills: Int, val deaths: Int)