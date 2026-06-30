package de.tectoast.emolga.domain.pokemon.model

import de.tectoast.emolga.utils.toShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
    val name: String,
    val num: Int = -1,
    val types: List<String> = listOf(),
    private val baseStats: Map<String, Int> = mapOf(),
    val formeOrder: List<String>? = null,
    val forme: String? = null,
    val baseSpecies: String? = null,
    val prevo: String? = null,
    val abilities: Map<String, String> = mapOf(),
    val eggGroups: List<String> = listOf(),
    val otherFormes: List<String>? = null,
    val cosmeticFormes: List<String>? = null,
    val genderRatio: Map<String, Double>? = null,
    val gender: String? = null,
    val heightm: Double = -1.0,
    val weightkg: Double = -1.0,
    val color: String = "ERROR",

    val requiredAbility: String? = null,
    val requiredItem: String? = null,
    val requiredMove: String? = null,

    val evoLevel: Int? = null,
    val evoType: String? = null,
    val evoMove: String? = null,
    val evoItem: String? = null,
    val evoCondition: String? = null,
    val evos: List<String>? = null,

    val isNonstandard: String? = null,
    val changesFrom: String? = null

) {
    fun calcSpriteName(): String {
        return buildString {
            append(baseSpeciesOrName.toShowdownID().value)
            if (forme != null) {
                append("-")
                append(forme.toShowdownID().value)
            }
        }
    }

    val baseSpeciesOrName: String
        get() = baseSpecies ?: name
}