package de.tectoast.emolga.domain.league.prediction.model

data class BasicUserPredictionScore(
    val userId: Long,
    val correctCount: Int
)
