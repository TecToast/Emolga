package de.tectoast.emolga.league.config

import de.tectoast.emolga.features.draft.InstantToStringSerializer
import de.tectoast.emolga.utils.DurationSerializer
import de.tectoast.emolga.utils.ReplayData
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration


@Serializable
data class ReplayDataStoreConfig(
    @Serializable(with = InstantToStringSerializer::class) val lastUploadStart: Instant,
    val lastGamesMadeReminder: GamesMadeReminder? = null,
    @Serializable(with = DurationSerializer::class) val intervalBetweenUploadAndVideo: Duration = Duration.ZERO,
    @Serializable(with = DurationSerializer::class) val intervalBetweenGD: Duration,
    @Serializable(with = DurationSerializer::class) val intervalBetweenMatches: Duration,
    val amount: Int,
    val withYT: Boolean = true,
)

@Serializable
data class ReplayDataStoreData(val data: MutableMap<Int, MutableMap<Int, ReplayData>> = mutableMapOf())

@Serializable
data class GamesMadeReminder(
    @Serializable(with = InstantToStringSerializer::class) val lastSend: Instant,
    val channel: Long
)