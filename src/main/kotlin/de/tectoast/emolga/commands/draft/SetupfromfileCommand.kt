package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.Draft

class SetupfromfileCommand :
    Command("setupfromfile", "Setzt einen Draft auf Basis einer Datei auf", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Draftname", "Der Name des Drafts", ArgumentManagerTemplate.Text.any())
            .add(
                "tc",
                "Text-Channel",
                "Der Text-Channel, wo die Teams drin stehen",
                ArgumentManagerTemplate.DiscordType.CHANNEL,
                true
            )
            .setExample("!setupfromfile Emolga-Conference #emolga-teamübersicht")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        e.deleteMessage()
        val args = e.arguments
        Draft(e.textChannel, args.getText("name"), if (args.has("tc")) args.getChannel("tc").id else null, true)
    }
}