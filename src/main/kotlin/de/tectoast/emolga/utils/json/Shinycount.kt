package de.tectoast.emolga.utils.json

import kotlinx.serialization.Serializable

@Serializable
class Shinycount(
    val names: Map<Long, String>,
    val counter: Map<String, MutableMap<String, Long>>,
    val methodorder: List<String>,
    val userorder: List<Long>
) {
    companion object {
        lateinit var get: Shinycount
    }
}