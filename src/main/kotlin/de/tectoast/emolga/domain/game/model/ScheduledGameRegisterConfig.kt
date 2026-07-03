package de.tectoast.emolga.domain.game.model

import de.tectoast.emolga.utils.serializer.DurationSerializer
import de.tectoast.emolga.utils.serializer.InstantToStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Serializable
data class ScheduledGameRegisterConfig(
    @Serializable(with = InstantToStringSerializer::class) val lastUploadStart: Instant,
    @Serializable(with = DurationSerializer::class) val gracePeriodForYT: Duration = Duration.ZERO,
    @Serializable(with = DurationSerializer::class) val intervalBetweenWeeks: Duration = 7.days,
    @Serializable(with = DurationSerializer::class) val intervalBetweenMatches: Duration = 1.hours,
    val ytEnableConfig: YTEnableConfig? = YTEnableConfig.WithDocEntry,
    val amount: Int,
    val hideResults: Boolean = true,
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