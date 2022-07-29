package de.tectoast.emolga.utils.records

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.Companion.getGerNameNoCheck
import de.tectoast.emolga.commands.Command.Companion.save
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color
import java.util.function.Consumer

object TypicalSets {
    val json: JSONObject = Command.load("./typicalsets.json")
    fun add(pokemon: String, movesList: Collection<String>, item: String?, ability: String?) {
        val mon: JSONObject = json.createOrGetJSON(pokemon)
        mon.increment("uses")
        movesList.forEach(Consumer { m: String? ->
            mon.createOrGetJSON("moves").increment(
                getGerNameNoCheck(
                    m!!
                )
            )
        })
        item?.run {
            mon.createOrGetJSON("items").increment(
                getGerNameNoCheck(this)
            )
        }
        ability?.run {
            mon.createOrGetJSON("abilities").increment(
                getGerNameNoCheck(
                    this
                )
            )
        }
    }

    @Synchronized
    fun save() {
        save(json, "typicalsets.json")
    }

    fun buildPokemon(pokemon: String?): MessageEmbed {
        if (!json.has(pokemon)) {
            return EmbedBuilder().setTitle("Dieses Pokemon ist noch nicht in den TypicalSets erfasst!")
                .setColor(Color.RED).build()
        }
        val mon: JSONObject = json.getJSONObject(pokemon)
        val uses = mon.getInt("uses").toDouble()
        val comp = compareByDescending<Map.Entry<String, Any>> { it.value as Int }
        return EmbedBuilder().addField(
            "Attacken",
            mon.optJSONObject("moves", JSONObject()).toMap().entries.sortedWith(comp)
                .map { (key, value): Map.Entry<String, Any> ->
                    val usesStr = (value as Int / uses * 100f).toString()
                    key + ": " + usesStr.substring(0, usesStr.length.coerceAtMost(5)) + "%"
                }.take(5).joinToString("\n"),
            true
        ).addField(
            "Items",
            mon.optJSONObject("items", JSONObject()).toMap().entries.sortedWith(comp)
                .map { (key, value): Map.Entry<String, Any> ->
                    val itemsStr = (value as Int / uses * 100f).toString()
                    key + ": " + itemsStr.substring(0, itemsStr.length.coerceAtMost(5)) + "%"
                }.take(5).joinToString("\n"),
            true
        ).addField(
            "Fähigkeiten",
            mon.optJSONObject("abilities", JSONObject()).toMap().entries.sortedWith(comp)
                .map { (key, value): Map.Entry<String, Any> ->
                    val abilitiesStr = (value as Int / uses * 100f).toString()
                    key + ": " + abilitiesStr.substring(0, abilitiesStr.length.coerceAtMost(5)) + "%"
                }.take(5).joinToString("\n"),
            true
        ).setColor(Color.CYAN).setTitle(pokemon).build()
    }
}