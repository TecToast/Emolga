package de.tectoast.emolga.domain.game.model

import de.tectoast.emolga.domain.league.util.model.LeagueResult

sealed interface GameSource {
    val leagueResult: LeagueResult?

    data class FromReplay(
        override val leagueResult: LeagueResult?,
        val url: String,
        val showdownUserNames: List<String>,
        val format: String
    ) : GameSource

    data class Direct(override val leagueResult: LeagueResult) : GameSource
}