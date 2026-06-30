package de.tectoast.emolga.domain.league.prediction.model


data class MissingVotesData(
    val data: Map<String, List<List<List<Long>>>>,
    val amount: Int
)