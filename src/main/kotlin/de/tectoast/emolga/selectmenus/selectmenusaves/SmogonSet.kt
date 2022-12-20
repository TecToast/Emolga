package de.tectoast.emolga.selectmenus.selectmenusaves

import de.tectoast.emolga.commands.notNullAppend
import de.tectoast.emolga.utils.json.showdown.Moveset
import de.tectoast.emolga.utils.json.showdown.Strategy
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class SmogonSet(private val strategies: List<Strategy>) {
    private var format: Strategy
    var set: Moveset

    init {
        format = strategies.first()
        set = format.movesets.first()
    }

    fun changeFormat(format: String) {
        this.format = strategies.first { it.format == format }
        set = this.format.movesets.first()
    }

    fun changeSet(set: String) {
        this.set = format.movesets.first { it.name == set }
    }

    companion object {
        val statnames =
            mapOf("hp" to "HP", "atk" to "Atk", "def" to "Def", "spa" to "SpA", "spd" to "SpD", "spe" to "Spe")
    }

    fun buildMessage(): String {
        return buildString {
            append("${set.pokemon} ")
            if ("No Item" !in set.items) {
                append(set.items.joinToString(" / ", prefix = "@ "))
            }
            append(set.abilities.joinToString(" / ", prefix = "\nAbility: "))
            append(set.evconfigs.first().entries.filter { it.value > 0 }
                .joinToString(" / ", prefix = "\nEVs: ") { "${it.value} ${statnames[it.key]}" })
            set.ivconfigs.firstOrNull()?.entries?.filter { it.value < 31 }
                ?.joinToString(" / ", prefix = "\nIVs: ") { "${it.value} ${statnames[it.key]}" }?.let {
                    append(it)
                }
            append(set.natures.joinToString(" / ", prefix = "\n", postfix = " Nature"))
            append(set.moveslots.joinToString("\n", prefix = "\n") { moves ->
                moves.joinToString(" / ", prefix = "- ") { move ->
                    move.move.notNullAppend(move.type?.let { " $it" })
                }
            })
        }
    }

    fun buildActionRows(id: String): List<ActionRow> = listOf(
        ActionRow.of(
            StringSelectMenu.create("smogonformat;$id").addOptions(strategies.map { it.format }.map {
                SelectOption.of("Format: $it", it).withDefault(format.format == it)
            }).build()
        ), ActionRow.of(
            StringSelectMenu.create("smogonset;$id").addOptions(format.movesets.map { it.name }
                .map { SelectOption.of("Set: $it", it).withDefault(set.name == it) }).build()
        )
    )
}
