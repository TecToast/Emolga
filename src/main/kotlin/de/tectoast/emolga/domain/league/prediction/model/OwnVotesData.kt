package de.tectoast.emolga.domain.league.prediction.model

data class OwnVotesData(
    val u1: List<Long>,
    val u2: List<Long>,
    val selected: List<Long>,
    val correct: Boolean?
)