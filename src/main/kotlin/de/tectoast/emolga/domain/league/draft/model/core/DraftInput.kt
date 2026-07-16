package de.tectoast.emolga.domain.league.draft.model.core

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DraftInput {
    val pokemon: ShowdownID
    val freesPokemon: ShowdownID?
}

@Serializable
@SerialName("Pick")
data class PickInput(
    override val pokemon: ShowdownID,
    val tier: String? = null,
    val free: Boolean = false,
    val tera: Boolean = false,
    val noCost: Boolean = false
) : DraftInput {
    override val freesPokemon = null
}

@Serializable
@SerialName("Switch")
data class SwitchInput(val oldmon: ShowdownID, override val pokemon: ShowdownID) : DraftInput {
    override val freesPokemon = oldmon

}

@Serializable
@SerialName("Ban")
data class BanInput(override val pokemon: ShowdownID) : DraftInput {
    override val freesPokemon = null

}
