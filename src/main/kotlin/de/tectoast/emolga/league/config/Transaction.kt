package de.tectoast.emolga.league.config

import de.tectoast.emolga.features.league.InstantToStringSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TransactionConfig(
    val maxPoints: Int,
    val maxGameday: Int,
    @Serializable(with = InstantToStringSerializer::class)
    val lastDocInsert: Instant? = null
)

@Serializable
data class LeagueTransactionData(
    val running: MutableMap<Int, MutableMap<Int, TransactionEntry>> = mutableMapOf(),
    val amounts: MutableMap<Int, TransactionAmounts> = mutableMapOf(),
)

@Serializable
data class TransactionAmounts(
    var mons: Int = 0,
    var extraTeras: Int = 0,
) {
    fun remaining(total: Int) = total - mons - extraTeras
}

@Serializable
data class TransactionEntry(
    /**
     * The dropped mons in tl names
     */
    val drops: MutableList<String> = mutableListOf(),
    /**
     * The picked mons in official names
     */
    val picks: MutableList<String> = mutableListOf(),
)