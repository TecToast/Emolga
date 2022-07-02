package de.tectoast.emolga.commands.admin

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import java.util.stream.Collectors

class ClearCommand : Command("clear", "Cleart den Channel", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        e.textChannel.deleteMessages(e.textChannel.iterableHistory.stream().collect(Collectors.toList())).queue()
    }
}