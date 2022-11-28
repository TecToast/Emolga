package de.tectoast.emolga.utils.json.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.toSDName
import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
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
    val genderRatio: Map<String, Double>? = null,
    val gender: String? = null,
    val heightm: Double,
    val weightkg: Double,

    val evoLevel: Int? = null,
    val evoType: String? = null,
    val evoMove: String? = null,
    val evoItem: String? = null,
    val evoCondition: String? = null,

    ) {

    val speed: Int
        get() = baseStats["spe"]!!

    fun buildStatString(): String {
        return baseStats.entries.joinToString("\n") { "${statNames[it.key]}: ${it.value}" } + "\nSumme: ${baseStats.values.sum()}"
    }

    val baseSpeciesAndForme: String
        get() = Command.getGerNameNoCheck(baseSpecies!!) + "-" + forme

    val baseSpeciesOrName: String
        get() = baseSpecies ?: name

    val formeSuffix: String?
        get() = if (forme != null) "-${forme.toSDName()}" else null

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
