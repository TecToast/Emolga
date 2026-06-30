package de.tectoast.emolga.domain.league.transaction.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class APITransactionData(
    val picked: List<TransactionPokemonData>,
    val available: List<TransactionPokemonData>,
    val teraCount: Int,
    val teraMaxPoints: Int? = null,
    val monMaxPoints: Int? = null,
    val transactionPoints: Int,
    val maxTransactionPoints: Int,
    val displayNames: Map<ShowdownID, String>
)