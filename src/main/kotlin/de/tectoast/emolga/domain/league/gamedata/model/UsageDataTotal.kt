package de.tectoast.emolga.domain.league.gamedata.model

import kotlinx.serialization.Serializable


@Serializable
data class UsageDataTotal(val total: Int, val maxGameday: Int, val allLeagues: List<String>, val data: List<UsageData>)