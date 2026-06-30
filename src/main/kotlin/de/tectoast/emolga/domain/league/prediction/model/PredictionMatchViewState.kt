package de.tectoast.emolga.domain.league.prediction.model

data class PredictionMatchViewState(
    val leagueName: String,
    val week: Int,
    val battleIndex: Int,
    val channelId: Long,
    val isLocked: Boolean,

    val idx1: Int,
    val idx2: Int,
    val player1Name: String,
    val player2Name: String,

    val embedDescription: String? = null,
    val embedColor: Int
)
