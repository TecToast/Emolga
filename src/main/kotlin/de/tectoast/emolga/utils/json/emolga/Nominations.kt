package de.tectoast.emolga.utils.json.emolga

import kotlinx.serialization.Serializable

@Serializable
data class Nominations(
    var currentDay: Int = 1,
    val nominated: MutableMap<Int, MutableMap<Int, List<Int>>> = mutableMapOf()
) {
    fun current() = nominated[currentDay]!!
}
