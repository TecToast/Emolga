package de.tectoast.emolga.selectmenus.selectmenusaves

import de.tectoast.emolga.commands.pokemon.SmogonCommand
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption

class SmogonSet(val arr: JSONArray) {
    private var format: JSONObject
    var set: JSONObject
    private var ev: JSONObject
    private var iv: JSONObject
    private var moveslots: JSONArray

    init {
        format = arr.getJSONObject(0)
        set = format.getJSONArray("movesets").getJSONObject(0)
        ev = set.getJSONArray("evconfigs").getJSONObject(0)
        iv = if (set.getJSONArray("ivconfigs").length() > 0) set.getJSONArray("ivconfigs")
            .getJSONObject(0) else JSONObject()
        moveslots = set.getJSONArray("moveslots")
    }

    fun changeFormat(format: String) {
        this.format = arr.toJSONList().first { o: JSONObject -> o.getString("format") == format }
        set = this.format.getJSONArray("movesets").getJSONObject(0)
        ev = set.getJSONArray("evconfigs").getJSONObject(0)
        iv = if (set.getJSONArray("ivconfigs").length() > 0) set.getJSONArray("ivconfigs")
            .getJSONObject(0) else JSONObject()
        moveslots = set.getJSONArray("moveslots")
    }

    fun changeSet(set: String) {
        this.set = format.getJSONList("movesets").first { o: JSONObject -> o.getString("name") == set }
        ev = this.set.getJSONArray("evconfigs").getJSONObject(0)
        iv = if (this.set.getJSONArray("ivconfigs").length() > 0) this.set.getJSONArray("ivconfigs")
            .getJSONObject(0) else JSONObject()
        moveslots = this.set.getJSONArray("moveslots")
    }

    fun buildMessage(): String {
        return """
                ${set.getString("pokemon")} @ ${set.getStringList("items").joinToString(" / ")}
                Ability: ${set.getStringList("abilities").joinToString(" / ")}
                EVs: ${
            ev.keySet().asSequence().filter { ev.getInt(it) > 0 }
                .map { "${ev.getInt(it)} ${SmogonCommand.statnames[it]}" }.joinToString(" / ")
        }${
            if (iv.length() == 0) "" else "IVs: ${
                iv.keySet().asSequence().filter { ev.getInt(it) > 0 }
                    .map { "${ev.getInt(it)} ${SmogonCommand.statnames[it]}" }.joinToString(" / ")
            }"
        }
                ${set.getStringList("natures").joinToString(" / ")} Nature
                - ${
            moveslots.getJSONArray(0).toJSONList().asSequence()
                .map { "${it.getString("move")}${if (!it.isNull("type")) " ${it.getString("type")}" else ""}" }
                .joinToString(" / ")
        }
                - ${
            moveslots.getJSONArray(1).toJSONList().asSequence()
                .map { "${it.getString("move")}${if (!it.isNull("type")) " ${it.getString("type")}" else ""}" }
                .joinToString(" / ")
        }
                - ${
            moveslots.getJSONArray(2).toJSONList().asSequence()
                .map { "${it.getString("move")}${if (!it.isNull("type")) " ${it.getString("type")}" else ""}" }
                .joinToString(" / ")
        }
                - ${
            moveslots.getJSONArray(3).toJSONList().asSequence()
                .map { "${it.getString("move")}${if (!it.isNull("type")) " ${it.getString("type")}" else ""}" }
                .joinToString(" / ")
        }""".trimIndent()
    }

    fun buildActionRows(): List<ActionRow> {
        return listOf(
            ActionRow.of(
                SelectMenu.create("smogonformat")
                    .addOptions(arr.toJSONList().map { o: JSONObject -> o.getString("format") }.map { s: String ->
                        SelectOption.of("Format: $s", s).withDefault(format.getString("format") == s)
                    }).build()
            ),
            ActionRow.of(
                SelectMenu.create("smogonset")
                    .addOptions(format.getJSONList("movesets").map { o: JSONObject -> o.getString("name") }
                        .map { s: String -> SelectOption.of("Set: $s", s).withDefault(set.getString("name") == s) })
                    .build()
            )
        )
    }
}