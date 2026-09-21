package de.tectoast.emolga.domain.league.prediction.model.config

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class PredictionGameLeaderboardConfig(
    val channel: Long,
    val topN: Int,
    val intervalAfterSend: Duration? = null
)