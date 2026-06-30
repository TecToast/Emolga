package de.tectoast.emolga.domain.league.transaction.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class TransactionPokemonData(
    val showdownId: ShowdownID,
    val tier: String,
    val teraTier: String?,
    var tera: Boolean = false,
    var picked: Boolean = false
)
