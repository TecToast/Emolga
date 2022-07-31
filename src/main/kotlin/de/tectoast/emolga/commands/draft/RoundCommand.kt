package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League

class RoundCommand : Command("round", "Zeigt die Runde des derzeitigen Drafts an", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        League.onlyChannel(e.textChannel)?.let { e.reply("Der Draft ist in Runde ${it.round}!") }
    }
}