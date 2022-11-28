package de.tectoast.emolga.utils.json.showdown

import kotlinx.serialization.Serializable

@Serializable
data class Strategy(
    val format: String,
    val movesets: List<Moveset>,
)

@Serializable
data class Moveset(
    val name: String,
    val pokemon: String,
    val items: List<String>,
    val abilities: List<String>,
    val evconfigs: List<Map<String, Int>>,
    val ivconfigs: List<Map<String, Int>>,
    val natures: List<String>,
    val moveslots: List<List<Moveslot>>,
)

@Serializable
data class Moveslot(
    val move: String,
    val type: String?,
)
