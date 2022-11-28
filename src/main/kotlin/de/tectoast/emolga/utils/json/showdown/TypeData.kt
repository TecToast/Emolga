package de.tectoast.emolga.utils.json.showdown

import kotlinx.serialization.Serializable

@Serializable
data class TypeData(val damageTaken: Map<String, Int>) {
    operator fun get(type: String) = damageTaken[type] ?: 0
}
