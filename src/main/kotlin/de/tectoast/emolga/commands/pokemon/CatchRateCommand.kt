package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import kotlin.math.pow

class CatchRateCommand :
    Command("catchrate", "Gibt die Catch-Rate des jeweiligen Pokemon aus", CommandCategory.Pokemon) {
    init {
        aliases += "cr"
        argumentTemplate = ArgumentManagerTemplate.create {
            add("mon", "Pokemon", "Das Mon lol", Translation.Type.POKEMON)
            example = "!catchrate Emolga"
        }
        slash(false, 821350264152784896)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val mon = e.arguments.getTranslation("mon").translation
        val cr = catchrates[mon] ?: return e.reply("Das Pokemon hat keine Catch-Rate!")
        e.reply(
            "**$mon** hat eine Catchrate von **$cr**! Die Chance mit einem normalen Pokéball beträgt somit **${
                ((1f / 3f) * cr.toFloat() / 255f).toDouble().pow(0.75).times(1000).times(10).div(10)
            }%**!"
        )
    }

}
