package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class GerCommand : Command("ger", "Zeigt den deutschen Namen dieser Sache.", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "stuff",
                "Pokemon|Attacke|Fähigkeit|Item|Typ",
                "Die Sache, von der du den englischen Namen haben möchtest",
                Translation.Type.all()
            )
            .setExample("!ger Primarina")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val stuff = e.arguments.getTranslation("stuff").translation
        e.reply(if (stuff == "Psychokinese") "Psychokinese/Psycho" else stuff)
    }
}