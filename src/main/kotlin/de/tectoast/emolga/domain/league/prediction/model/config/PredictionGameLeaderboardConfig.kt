package de.tectoast.emolga.domain.league.prediction.model.config

import kotlinx.serialization.Serializable

@Serializable
data class PredictionGameLeaderboardConfig(
    val channel: Long,
    val topN: Int
)