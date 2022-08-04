package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class EmoteStealCommand : Command("emotesteal", "Stealt Emotes... lol", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        emoteSteal.remove(tco.idLong).also {
            if (!it) emoteSteal.add(tco.idLong)
            e.reply("Der EmoteSteal wurde ${if (it) "de" else ""}aktiviert!")
        }
    }
}