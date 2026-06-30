package de.tectoast.emolga.domain.league.util.model

import de.tectoast.emolga.domain.league.config.model.LeagueConfig

data class LeagueQueryResult(val leagueName: String, val config: LeagueConfig, val idx: Int)