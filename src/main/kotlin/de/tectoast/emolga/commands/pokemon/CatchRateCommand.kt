package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import kotlin.math.pow
import kotlin.math.round

class CatchRateCommand :
    Command("catchrate", "Gibt die Catch-Rate des jeweiligen Pokemon aus", CommandCategory.Pokemon) {
    init {
        aliases += "cr"
        argumentTemplate = ArgumentManagerTemplate.create {
            add("mon", "Pokemon", "Das Mon lol", Translation.Type.POKEMON)
            example = "!catchrate Emolga"
        }
    }

    override suspend fun process(e: GuildCommandEvent) {
        val mon = e.arguments.getTranslation("mon").translation
        val cr = catchrates.getInt(mon)
        val x = (1f / 3f) * cr.toFloat()
        val chance = (x / 255f).toDouble().pow(0.75).times(100).round(1)
        e.reply("**$mon** hat eine Catchrate von **$cr**! Die Chance mit einem normalen Pokéball beträgt somit **$chance%**!")
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }
}