package de.tectoast.emolga.domain.league.prediction.model.config

import de.tectoast.emolga.utils.serializer.ColorToStringSerializer
import de.tectoast.emolga.utils.serializer.DurationSerializer
import de.tectoast.emolga.utils.serializer.InstantToStringSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class PredictionGameConfig(
    @Serializable(with = InstantToStringSerializer::class) val lastSending: Instant,
    @Serializable(with = InstantToStringSerializer::class) val lastLockButtons: Instant? = null,
    @Serializable(with = DurationSerializer::class) val interval: Duration,
    val amount: Int,
    val channel: Long,
    @Serializable(with = ColorToStringSerializer::class)
    val customEmbedColor: Int? = null,
    val roleToPing: Long? = null,
    val currentState: PredictionGameCurrentStateType? = null,
    val leaderboardConfig: PredictionGameLeaderboardConfig? = null
)


