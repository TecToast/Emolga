package de.tectoast.emolga.utils.json.showdown

import de.tectoast.emolga.utils.notNullAppend
import de.tectoast.emolga.utils.toSDName
import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
    val id: String = "ERROR",
    val name: String,
    val num: Int,
    val types: List<String>,
    val baseStats: Map<String, Int>,
    val formeOrder: List<String>? = null,
    val forme: String? = null,
    val baseSpecies: String? = null,
    val prevo: String? = null,
    val abilities: Map<String, String>,
    val eggGroups: List<String>,
    val otherFormes: List<String>? = null,
    val cosmeticFormes: List<String>? = null,
    val genderRatio: Map<String, Double>? = null,
    val gender: String? = null,
    val heightm: Double,
    val weightkg: Double,

    val requiredAbility: String? = null,
    val requiredItem: String? = null,
    val requiredMove: String? = null,

    val evoLevel: Int? = null,
    val evoType: String? = null,
    val evoMove: String? = null,
    val evoItem: String? = null,
    val evoCondition: String? = null,

    ) {

    val speed: Int
        get() = baseStats["spe"]!!

    fun getGen5Sprite(): String {
        return buildString {
            append("=IMAGE(\"https://play.pokemonshowdown.com/sprites/gen5/")
            append(
                (baseSpecies ?: name).toSDName().notNullAppend(forme?.toSDName()?.let { "-$it" })
            )
            append(".png\"; 1)")
        }
    }

    companion object {
        val statNames = mapOf(
            "hp" to "HP",
            "atk" to "Atk",
            "def" to "Def",
            "spa" to "SpAtk",
            "spd" to "SpDef",
            "spe" to "Init"
        )
    }
}
