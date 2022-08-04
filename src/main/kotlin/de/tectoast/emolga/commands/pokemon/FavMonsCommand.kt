package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class FavMonsCommand : Command("favmons", "Erstelle eine Liste deiner Fav Mons", CommandCategory.Pokemon) {
    init {
        wip()
    }

    override suspend fun process(e: GuildCommandEvent) {}

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }
}