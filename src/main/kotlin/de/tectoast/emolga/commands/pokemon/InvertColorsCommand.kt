package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class InvertColorsCommand :
    Command("invertcolors", "Zeigt einen Sprite in der invertierten Farbe", CommandCategory.Pokemon) {
    init {
        aliases.add("invertcolours")
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "shiny",
                "Shiny",
                "",
                ArgumentManagerTemplate.Text.of(
                    SubCommand.of(
                        "Shiny",
                        "Wenn das Pokemon als Shiny angezeigt werden soll"
                    )
                ),
                true
            )
            .addEngl("mon", "Pokemon", "Das Pokemon lol", Translation.Type.POKEMON)
            .setExample("!invertcolors Zygarde")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val f = invertImage(args.getTranslation("mon").translation, args.isText("shiny", "Shiny"))
        e.textChannel.sendFile(f).queue { f.delete() }
    }
}