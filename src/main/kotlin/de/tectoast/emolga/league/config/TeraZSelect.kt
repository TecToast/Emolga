package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class TeraZSelectConfig(val tiers: Set<String> = emptySet(), val type: TeraZType = TeraZType.Z)

@Serializable
data class TeraZSelectData(var mid: String? = null, val selected: MutableMap<Int, MutableSet<String>> = mutableMapOf())

enum class TeraZType {
    Tera, Z
}