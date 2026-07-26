package de.tectoast.emolga.domain.league.prediction.model

data class PredictionMatchViewState(
    val leagueName: String,
    val week: Int,
    val battleIndex: Int,
    val channelId: Long,
    val isLocked: Boolean,

    val players: List<PredictionMatchViewPlayerState>,

    val embedDescription: String? = null,
    val embedColor: Int,
)
