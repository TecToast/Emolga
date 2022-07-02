package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.IntColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color

object DasorUsageManager : DataManager("dasorusage") {
    private val POKEMON = StringColumn("pokemon", this)
    private val USES = IntColumn("uses", this)

    init {
        setColumns(POKEMON, USES)
    }

    fun addPokemon(pokemon: String) {
        addStatistics(pokemon, 1)
    }

    fun buildMessage(): MessageEmbed {
        return EmbedBuilder()
            .setTitle("Dasor Statistik mit coolen Mons")
            .setDescription(read(selectAll() + " ORDER BY uses DESC", ResultsFunction { results ->
                val b = StringBuilder()
                while (results.next()) {
                    b.append(results.getString("pokemon")).append(": ")
                        .append(results.getInt("uses")).append("\n")
                }
                b.toString()
            }))
            .setColor(Color.CYAN)
            .build()
    }
}