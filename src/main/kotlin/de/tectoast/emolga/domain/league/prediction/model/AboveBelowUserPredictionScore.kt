package de.tectoast.emolga.domain.league.prediction.model

data class AboveBelowUserPredictionScore(
    val rank: Int,
    val userId: Long,
    val correctVotes: Int,
    val totalVotes: Int,
    val isTarget: Boolean
)