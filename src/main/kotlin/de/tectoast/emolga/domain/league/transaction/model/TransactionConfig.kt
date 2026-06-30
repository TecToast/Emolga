package de.tectoast.emolga.domain.league.transaction.model

import de.tectoast.emolga.utils.serializer.DurationSerializer
import de.tectoast.emolga.utils.serializer.InstantToStringSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@Serializable
data class TransactionConfig(
    val maxPoints: Int,
    val maxWeek: Int,
    @Serializable(with = InstantToStringSerializer::class)
    val lastDocInsert: Instant? = null,
    @Serializable(with = DurationSerializer::class)
    val interval: Duration = 7.days
)
