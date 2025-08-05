package de.tectoast.emolga.league.config

import de.tectoast.emolga.features.draft.InstantToStringSerializer
import de.tectoast.emolga.utils.DurationSerializer
import de.tectoast.emolga.utils.ReplayData
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours


@Serializable
data class ReplayDataStoreConfig(
    @Serializable(with = InstantToStringSerializer::class) val lastUploadStart: Instant,
    val lastGamesMadeReminder: GamesMadeReminder? = null,
    @Serializable(with = DurationSerializer::class) val gracePeriodForYT: Duration = Duration.ZERO,
    @Serializable(with = DurationSerializer::class) val intervalBetweenGD: Duration = 7.days,
    @Serializable(with = DurationSerializer::class) val intervalBetweenMatches: Duration = 1.hours,
    val ytEnableConfig: YTEnableConfig = YTEnableConfig.WithDocEntry,
    val amount: Int,
)

@Serializable
sealed interface YTEnableConfig {
    @Serializable
    @SerialName("WithDocEntry")
    data object WithDocEntry : YTEnableConfig

    @Serializable
    @SerialName("Custom")
    data class Custom(
        @Serializable(with = InstantToStringSerializer::class) val lastUploadStart: Instant,
        @Serializable(with = DurationSerializer::class) val intervalBetweenMatches: Duration = 1.hours,
    ) : YTEnableConfig
}

@Serializable
data class ReplayDataStoreData(val data: MutableMap<Int, MutableMap<Int, ReplayData>> = mutableMapOf())

@Serializable
data class GamesMadeReminder(
    @Serializable(with = InstantToStringSerializer::class) val lastSend: Instant,
    val channel: Long
)