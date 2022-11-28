package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.utils.sql.managers.FullStatsManager
import dev.minn.jda.ktx.messages.Embed
import kotlin.math.roundToInt

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
        e.reply(
            Embed {
                color = embedColor
                title = "Gesamtstatistik von $mon in ${replayCount.get() - 6212} Replays"
                field("Kills", data.kills.toString(), false)
                field("Deaths", data.deaths.toString(), false)
                field("Uses", data.uses.toString(), false)
                field("Wins", data.wins.toString(), false)
                field("Looses", data.looses.toString(), false)
                field(
                    "Kills/Use",
                    "${(data.kills.toDouble() / data.uses.toDouble() * 100.0).roundToInt() / 100.0}",
                    false
                )
                field(
                    "Wins/Use",
                    "${(data.wins.toDouble() / data.uses.toDouble() * 100.0).roundToInt() / 100.0}",
                    false
                )
            }
        )
    }
}
