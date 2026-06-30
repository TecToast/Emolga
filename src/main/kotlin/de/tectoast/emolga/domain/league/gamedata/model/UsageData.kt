package de.tectoast.emolga.domain.league.gamedata.model

import kotlinx.serialization.Serializable

@Serializable
data class UsageData(val mon: String, val count: Int)
