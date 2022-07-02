package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class EmoteStealCommand : Command("emotesteal", "Stealt Emotes... lol", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        if (emoteSteal.remove(tco.idLong)) {
            tco.sendMessage("Der EmoteSteal wurde deaktiviert!").queue()
        } else {
            emoteSteal.add(tco.idLong)
            tco.sendMessage("Der EmoteSteal wurde aktiviert!").queue()
        }
    }
}