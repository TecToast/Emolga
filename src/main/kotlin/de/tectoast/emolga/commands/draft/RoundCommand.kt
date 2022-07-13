package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.Draft

class RoundCommand : Command("round", "Zeigt die Runde des derzeitigen Drafts an", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val op = Draft.drafts.stream().filter { d: Draft -> d.tc.id == e.textChannel.id }.findFirst()
        if (op.isEmpty) {
            e.reply("In diesem Textchannel findet derzeit kein Draft statt!")
            return
        }
        e.reply("Der Draft ist in Runde " + op.get().round + "!")
    }
}