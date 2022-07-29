package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.Draft

class StopdraftCommand : Command("stopdraft", "Beendet den Draft", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("draftname", "Draftname", "Der Name des Drafts", ArgumentManagerTemplate.draft())
            .setExample("!stopdraft Emolga-Conference")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        if (Draft.drafts.removeIf { d ->
                (d.name == e.arguments.getText("draftname")).also { if (it) d.cooldown!!.cancel(false) }
            }) {
            e.reply("Dieser Draft wurde beendet!")
        } else {
            e.reply("Dieser Draft existiert nicht!")
        }
    }
}