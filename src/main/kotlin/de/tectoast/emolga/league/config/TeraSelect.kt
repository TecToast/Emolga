package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class TeraSelectConfig(val tiers: Set<String> = emptySet())

@Serializable
data class TeraSelectData(var mid: String? = null, val selected: MutableMap<Int, MutableSet<String>> = mutableMapOf())