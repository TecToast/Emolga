package de.tectoast.emolga.utils.records

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.Companion.getGerNameNoCheck
import de.tectoast.emolga.commands.Command.Companion.save
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.commands.increment
import dev.minn.jda.ktx.messages.Embed
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.MessageEmbed
import kotlin.math.roundToInt

object TypicalSets {
    val json: MutableMap<String, TypicalSet> =
        Command.load<Map<String, TypicalSet>>("./typicalsets.json").toMutableMap()

    fun add(pokemon: String, movesList: Collection<String>, item: String?, ability: String?) {
        val mon = json.getOrPut(pokemon) { TypicalSet(0, mutableMapOf(), mutableMapOf(), mutableMapOf()) }
        mon.uses++
        movesList.forEach {
            mon.moves.increment(getGerNameNoCheck(it))
        }
        item?.let {
            mon.items.increment(getGerNameNoCheck(it))
        }
        ability?.let {
            mon.abilities.increment(getGerNameNoCheck(it))
        }
    }

    @Synchronized
    fun save() {
        save(json, "typicalsets.json")
    }

    private val comp = compareByDescending<Map.Entry<String, Int>> { it.value }

    private fun MutableMap<String, Int>.max5(uses: Double) = entries.sortedWith(comp).take(5)
        .joinToString("\n") { "${it.key}: ${(it.value.toDouble() / uses * 10000.0).roundToInt() / 100.0}%" }

    fun buildPokemon(pokemon: String): MessageEmbed {
        val mon = json[pokemon] ?: return Embed(
            title = "Dieses Pokemon ist noch nicht in den TypicalSets erfasst!",
            color = 0xFF0000
        )
        val uses = mon.uses.toDouble()
        return Embed {
            title = "TypicalSets für $pokemon"
            field {
                name = "Attacken"
                value = mon.moves.max5(uses)
                inline = true
            }
            field {
                name = "Items"
                value = mon.items.max5(uses)
                inline = true
            }
            field {
                name = "Fähigkeiten"
                value = mon.abilities.max5(uses)
                inline = true
            }
            color = embedColor
        }
    }
}

@Serializable
data class TypicalSet(
    var uses: Int = 0,
    val moves: MutableMap<String, Int> = mutableMapOf(),
    val items: MutableMap<String, Int> = mutableMapOf(),
    val abilities: MutableMap<String, Int> = mutableMapOf()
)
