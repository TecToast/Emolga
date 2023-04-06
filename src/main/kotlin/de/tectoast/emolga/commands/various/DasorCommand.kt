package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.exposed.DasorUsageDB

class DasorCommand : Command("dasor", "ist cool", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.reply(DasorUsageDB.buildMessage())
    }
}
