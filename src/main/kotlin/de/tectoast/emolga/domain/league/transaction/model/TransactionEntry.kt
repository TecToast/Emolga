package de.tectoast.emolga.domain.league.transaction.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class TransactionEntry(
    /**
     * The dropped mons in tl names
     */
    val drops: MutableList<ShowdownID> = mutableListOf(),
    /**
     * The picked mons in official names
     */
    val picks: MutableList<ShowdownID> = mutableListOf(),
)