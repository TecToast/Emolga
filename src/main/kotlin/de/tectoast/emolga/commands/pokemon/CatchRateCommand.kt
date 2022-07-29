package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class CatchRateCommand :
    Command("catchrate", "Gibt die Catch-Rate des jeweiligen Pokemon aus", CommandCategory.Pokemon) {
    init {
        aliases.add("cr")
        argumentTemplate =
            ArgumentManagerTemplate.builder().add("mon", "Pokemon", "Das Mon lol", Translation.Type.POKEMON)
                .setExample("!catchrate Primarene").build()
    }

    override fun process(e: GuildCommandEvent) {
        val mon = e.arguments.getTranslation("mon").translation
        e.reply("**" + mon + "** hat eine Catchrate von **" + catchrates.getString(mon) + "**!")
    }
}