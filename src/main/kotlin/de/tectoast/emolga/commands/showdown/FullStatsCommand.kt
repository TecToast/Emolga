package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.FullStatsManager
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import kotlin.math.min

class FullStatsCommand :
    Command("fullstats", "Zeigt die volle Statistik von einem Pokemon an (Kills/Uses/etc)", CommandCategory.Showdown) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "mon", "Pokemon", "Das Pokemon",
                ArgumentManagerTemplate.draftPokemon(), false, "Das ist kein Pokemon!"
            )
            .setExample("!fullstats Primarina")
            .build()
        slash()
    }

    override fun process(e: GuildCommandEvent) {
        val mon = e.arguments!!.getText("mon")
        val data = FullStatsManager.getData(mon)
        var kpu = (data.kills.toDouble() / data.uses.toDouble()).toString()
        kpu = kpu.substring(0, kpu.indexOf('.') + min(kpu.length - 1, 6))
        var wpu = (data.wins.toDouble() / data.uses.toDouble()).toString()
        wpu = wpu.substring(0, wpu.indexOf('.') + min(wpu.length - 1, 6))
        e.reply(
            EmbedBuilder().setColor(Color.CYAN)
                .setTitle("Gesamtstatistik von " + mon + " in " + (replayCount.get() - 6212) + " Replays")
                .addField("Kills", data.kills.toString(), false)
                .addField("Deaths", data.deaths.toString(), false)
                .addField("Uses", data.uses.toString(), false)
                .addField("Wins", data.wins.toString(), false)
                .addField("Looses", data.looses.toString(), false)
                .addField("Kills/Uses", kpu, false)
                .addField("Wins/Uses", wpu, false)
                .build()
        )
    }
}