package de.tectoast.emolga.utils.json.emolga

import kotlinx.serialization.Serializable

@Serializable
data class Nominations(var currentDay: Int, val nominated: MutableMap<Int, MutableMap<Long, String>>) {
    fun current() = nominated[currentDay]!!
}