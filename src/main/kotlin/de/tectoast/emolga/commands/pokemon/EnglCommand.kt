package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class EnglCommand : Command("engl", "Zeigt den englischen Namen dieser Sache.", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl(
                "stuff",
                "Pokemon|Attacke|Fähigkeit|Item",
                "Die Sache, von der du den englischen Namen haben möchtest",
                Translation.Type.all()
            )
            .setExample("!engl Primarene")
            .build()
    }

    override fun process(e: GuildCommandEvent) = e.reply(e.arguments.getTranslation("stuff").translation)
}