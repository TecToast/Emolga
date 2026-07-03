package de.tectoast.emolga.domain.league.prediction.model

import kotlinx.serialization.Serializable

@Serializable
data class PredictionGameVoteData(val userId: Long, val week: Int, val battle: Int, val idx: Int, val correct: Boolean?)