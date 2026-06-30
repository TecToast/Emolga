package de.tectoast.emolga.domain.league.prediction.model

data class AdvancedUserPredictionScore(
    val userId: Long,
    val correctVotes: Int,
    val totalVotes: Int
)