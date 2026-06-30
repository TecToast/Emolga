package de.tectoast.emolga.domain.league.transaction.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable


@Serializable
data class TransactionRequestData(
    val picks: Set<ShowdownID>,
    val drops: Set<ShowdownID>,
    val teraUsers: Set<ShowdownID>,
)
