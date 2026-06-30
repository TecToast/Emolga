package de.tectoast.emolga.domain.league.draft.model.random

import kotlinx.serialization.Serializable

@Serializable
data class RandomLeagueData(
    var currentMon: RandomLeaguePick? = null, val usedJokers: MutableMap<Int, Int> = mutableMapOf()
)
