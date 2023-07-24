package de.tectoast.emolga.utils.json.emolga

import kotlinx.serialization.Serializable

@Serializable
data class Nominations(
    var currentDay: Int = 1,
    val nominated: MutableMap<Int, MutableMap<Long, String>> = mutableMapOf()
) {
    fun current() = nominated[currentDay]!!
}
