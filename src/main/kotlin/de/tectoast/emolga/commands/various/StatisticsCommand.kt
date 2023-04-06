package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.database.exposed.StatisticsDB
import dev.minn.jda.ktx.messages.Embed

class StatisticsCommand :
    Command("statistics", "Zeigt Statistiken über die Usage des Bots an", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.reply(
            Embed(title = "Anzahl der Nutzung", color = embedColor, description = StatisticsDB.buildDescription(e))
        )
    }
}
