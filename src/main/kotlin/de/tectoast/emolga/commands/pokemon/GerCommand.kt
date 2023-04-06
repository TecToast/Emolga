package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.Translation

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

    override suspend fun process(e: GuildCommandEvent) {
        val stuff = e.arguments.getTranslation("stuff").translation
        e.reply(if (stuff == "Psychokinese") "Psychokinese/Psycho" else stuff)
    }
}
