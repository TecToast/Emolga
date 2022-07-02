package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.StatisticsManager
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

class StatisticsCommand :
    Command("statistics", "Zeigt Statistiken über die Usage des Bots an", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        e.reply(
            EmbedBuilder().setColor(Color.CYAN).setTitle("Anzahl der Nutzung")
                .setDescription(StatisticsManager.buildDescription(e)).build()
        )
    }
}