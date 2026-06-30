package de.tectoast.emolga.domain.league.transaction.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionAmounts(
    var mons: Int = 0,
    var extraTeras: Int = 0,
) {
    fun remaining(total: Int) = total - mons - extraTeras
}
