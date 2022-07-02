package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class Rule34Command : Command("rule34", "Schickt ein Rule34-Bild dieses Mons", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noSpecifiedArgs("!rule34 <Pokemon>", "!rule34 Guardevoir")
    }

    override fun process(e: GuildCommandEvent) {
        e.reply("Du Schlingel :^)")
        e.reply("https://tenor.com/view/dance-moves-dancing-singer-groovy-gif-17029825")
        sendToMe("Haha " + e.member.asMention + " hat in " + e.textChannel.asMention + " dumme sachen gemacht lmao")
    }
}