package de.tectoast.emolga.utils.json.showdown

import de.tectoast.emolga.utils.notNullAppend
import de.tectoast.emolga.utils.toSDName
import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
    val id: String = "ERROR",
    val name: String = "ERROR",
    val num: Int = -1,
    val types: List<String> = listOf(),
    val baseStats: Map<String, Int> = mapOf(),
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

    fun getIcon(): String {
        val str = specialCases[name] ?: ("$num".padStart(3, '0') + (forme?.split("-")?.let { arr ->
            if (arr[0] in specialForms) arr.last() else arr.first()
        }?.substring(0, 1)?.lowercase()?.let { x -> "-$x" } ?: ""))
        return "=IMAGE(\"https://www.serebii.net/pokedex-sv/icon/$str.png\";1)"
    }

    val baseSpeciesOrName: String
        get() = baseSpecies ?: name

    val spriteName: String get() = baseSpeciesOrName.toSDName().notNullAppend(forme?.toSDName()?.let { "-$it" })

    companion object {
        val statNames = mapOf(
            "hp" to "HP",
            "atk" to "Atk",
            "def" to "Def",
            "spa" to "SpAtk",
            "spd" to "SpDef",
            "spe" to "Init"
        )
        private val specialForms = listOf("Alola", "Galar", "Mega", "Paldea")
        private val specialCases =
            mapOf("Rotom-Fan" to "479-s", "Tauros-Paldea-Combat" to "128-p", "Zygarde-10%" to "718-10")
    }
}
