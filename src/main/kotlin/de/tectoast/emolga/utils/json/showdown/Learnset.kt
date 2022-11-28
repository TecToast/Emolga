package de.tectoast.emolga.utils.json.showdown

import kotlinx.serialization.Serializable

@Serializable
data class Learnset(val learnset: Map<String, List<String>>? = null) {
    operator fun invoke() = learnset!!
}
