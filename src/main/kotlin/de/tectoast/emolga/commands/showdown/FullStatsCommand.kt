package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.utils.sql.managers.FullStatsManager
import dev.minn.jda.ktx.messages.Embed
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

    override suspend fun process(e: GuildCommandEvent) {
        val mon = e.arguments.getText("mon")
        val data = FullStatsManager.getData(mon)
        var kpu = (data.kills.toDouble() / data.uses.toDouble()).toString()
        kpu = kpu.substring(0, kpu.indexOf('.') + min(kpu.length - 1, 6))
        var wpu = (data.wins.toDouble() / data.uses.toDouble()).toString()
        wpu = wpu.substring(0, wpu.indexOf('.') + min(wpu.length - 1, 6))
        e.reply(
            Embed {
                color = embedColor
                title = "Gesamtstatistik von $mon in ${replayCount.get() - 6212} Replays"
                field("Kills", data.kills.toString(), false)
                field("Deaths", data.deaths.toString(), false)
                field("Uses", data.uses.toString(), false)
                field("Wins", data.wins.toString(), false)
                field("Looses", data.looses.toString(), false)
                field("Kills/Use", kpu, false)
                field("Wins/Use", wpu, false)
            }
        )
    }
}