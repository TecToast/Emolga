package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class FloHelpCommand : Command(
    "flohelp",
    "Nutzt diesen Command, falls irgendwelche Fehler auftreten sollen, um meinen Programmierer Flo zu benachrichtigen",
    CommandCategory.Various
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("text", "Nachricht", "Die Nachricht, die du schicken willst", ArgumentManagerTemplate.Text.any())
            .setExample("!flohelp Ey, Emolga tut grad nich D:")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        sendToMe(
            "${e.textChannel.asMention} - ${e.member.asMention}:\n${e.message!!.contentRaw.substring(9)}"
        )
    }
}