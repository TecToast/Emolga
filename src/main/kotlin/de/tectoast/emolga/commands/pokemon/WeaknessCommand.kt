package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import java.util.stream.Collectors

class WeaknessCommand :
    Command("weakness", "Zeigt die Schwächen und Resistenzen eines Pokémons an", CommandCategory.Pokemon) {
    private val immunities: MutableMap<String, List<String>> = HashMap()
    private val resistances: MutableMap<String, Map<String, Int>> = HashMap()

    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "regform", "Form", "", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true
            )
            .add("stuff", "Pokemon", "Pokemon/Item/Whatever", Translation.Type.POKEMON)
            .add(
                "form",
                "Sonderform",
                "Sonderform, bspw. `Heat` bei Rotom",
                ArgumentManagerTemplate.Text.any(),
                true
            )
            .setExample("!weakness Primarina")
            .build()
        immunities["Electric"] = listOf("Elektro", "Lightning Rod", "Volt Absorb", "Motor Drive")
        immunities["Fire"] = listOf("Feuer", "Flash Fire")
        immunities["Water"] = listOf("Wasser", "Water Absorb", "Storm Drain", "Dry Skin")
        immunities["Ground"] = listOf("Boden", "Levitate")
        immunities["Grass"] = listOf("Pflanze", "Sap Sipper")
        resistances["Fire"] = java.util.Map.of("Fluffy", 1, "Thick Fat", -1)
        resistances["Ice"] = java.util.Map.of("Thick Fat", -1)
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments!!
        val gerName = args.getTranslation("stuff")
        val name = gerName.translation
        val mon = dataJSON.optJSONObject(
            toSDName(
                gerName.otherLang + args.getOrDefault(
                    "regform",
                    ""
                ) + args.getOrDefault("form", "") + (gerName.forme ?: "")
            )
        )
        val abijson = mon!!.getJSONObject("abilities")
        val abilities = abijson.keySet().stream().map { key: String? -> abijson.getString(key) }
            .collect(Collectors.toList())
        val oneabi = abilities.size == 1
        val types = mon.getStringList("types").toTypedArray()
        val x2: MutableSet<String> = LinkedHashSet()
        val x05: MutableSet<String> = LinkedHashSet()
        val x0: MutableSet<String> = LinkedHashSet()
        val immuneAbi = HashMap<String, String>()
        val changeAbi = HashMap<String, EffectivenessChangeText>()
        for (type in typeJSON.keySet()) {
            if (getImmunity(type, *types)) x0.add(
                (Translation.Type.TYPE.validate(
                    type,
                    Translation.Language.GERMAN,
                    "default"
                ) as Translation?)!!.translation
            ) else {
                if (oneabi && immunities.containsKey(type) && immunities[type]!!.contains(abilities[0])) {
                    x0.add(immunities[type]!![0] + " **(wegen " + abilities[0] + ")**")
                } else {
                    val abii = checkAbiImmunity(type, abilities)
                    if (abii != null && abii.isNotBlank()) {
                        immuneAbi[abii] = (Translation.Type.TYPE.validate(
                            type,
                            Translation.Language.GERMAN,
                            "default"
                        ) as Translation?)!!.translation
                    }
                    val typeMod = getEffectiveness(type, *types)
                    if (typeMod != 0) {
                        val t = (Translation.Type.TYPE.validate(
                            type,
                            Translation.Language.GERMAN,
                            "default"
                        ) as Translation?)!!.translation
                        when (typeMod) {
                            1 -> x2.add(t)
                            2 -> x2.add("**$t**")
                            -1 -> x05.add(t)
                            -2 -> x05.add("**$t**")
                        }
                    }
                    val pl = checkAbiChanges(type, abilities)
                    for (p in pl) {
                        val modified: Int = typeMod + p.value
                        val abi: String = p.ability
                        if (modified != typeMod && abi.isNotBlank()) {
                            val t = (Translation.Type.TYPE.validate(
                                type,
                                Translation.Language.GERMAN,
                                "default"
                            ) as Translation?)!!.translation
                            when (modified) {
                                2 -> changeAbi[t] = EffectivenessChangeText(abi, "vierfach-effektiv")
                                1 -> changeAbi[t] = EffectivenessChangeText(abi, "sehr effektiv")
                                0 -> changeAbi[t] = EffectivenessChangeText(abi, "neutral-effektiv")
                                -1 -> changeAbi[t] = EffectivenessChangeText(abi, "resistiert")
                                -2 -> changeAbi[t] = EffectivenessChangeText(abi, "vierfach-resistiert")
                            }
                        }
                    }
                }
            }
        }
        e.reply("""
    **$name**:
    Schwächen: ${java.lang.String.join(", ", x2)}
    Resistenzen: ${java.lang.String.join(", ", x05)}
    """.trimIndent()
                + (if (x0.size > 0) """
     
     Immunitäten: ${java.lang.String.join(", ", x0)}
     """.trimIndent() else "")
                + "\n" + immuneAbi.keys.stream().map { s: String -> getGerNameNoCheck(s) }
            .map { s: String -> "Wenn " + name + " **" + s + "** hat, wird der Typ **" + immuneAbi[s] + "** immunisiert." }
            .collect(Collectors.joining("\n"))
                + "\n" + changeAbi.keys.stream().map { s: String -> getGerNameNoCheck(s) }
            .map { s: String -> "Wenn " + name + " **" + changeAbi[s]!!.ability + "** hat, wird der Typ **" + s + " " + changeAbi[s]!!.value + "**." }
            .collect(Collectors.joining("\n"))
        )
    }

    private fun checkAbiChanges(type: String, abilities: List<String>): List<EffectivenessChangeNum> {
        if (!resistances.containsKey(type)) return emptyList()
        val l = resistances[type]!!
        return l.keys.stream().filter { o: String -> abilities.contains(o) }
            .map { s: String -> EffectivenessChangeNum(s, l[s]!!) }.collect(Collectors.toList())
    }

    private inner class EffectivenessChangeNum(val ability: String, val value: Int)
    private inner class EffectivenessChangeText(val ability: String, val value: String)

    private fun checkAbiImmunity(type: String, abilities: List<String>): String? {
        if (!immunities.containsKey(type)) return null
        val l = immunities[type]!!
        return l.stream().filter { o: String -> abilities.contains(o) }.collect(Collectors.joining(" oder "))
    }

    companion object {
        private fun getImmunity(type: String, vararg against: String): Boolean {
            if (against.size > 1) {
                for (s in against) {
                    if (getImmunity(type, s)) return true
                }
                return false
            }
            return typeJSON.getJSONObject(against[0]).getJSONObject("damageTaken").getInt(type) == 3
        }

        @JvmStatic
        fun getEffectiveness(type: String?, vararg against: String?): Int {
            var totalTypeMod = 0
            if (against.size > 1) {
                for (s in against) {
                    totalTypeMod += getEffectiveness(type, s)
                }
                return totalTypeMod
            }
            val typeData = typeJSON.optJSONObject(against[0]) ?: return 0
            return when (typeData.getJSONObject("damageTaken").getInt(type)) {
                1 -> 1
                2 -> -1
                else -> 0
            }
        }
    }
}