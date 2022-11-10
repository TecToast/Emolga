package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.AnalysisManager
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel

class ReplayChannelCommand : Command(
    "replaychannel",
    "Konfiguriert die Replay-Channel",
    CommandCategory.Showdown
) {
    init {
        everywhere = true
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, -1)
    }

    class Add : Command("add", "Fügt einen Replaychannel hinzu, standardmäßig dieser Channel") {
        init {
            argumentTemplate = ArgumentManagerTemplate.builder().add(
                "channel",
                "Channel",
                "Der Channel, wo die Ergebnisse reingeschickt werden sollen (optional)",
                ArgumentManagerTemplate.DiscordType.CHANNEL,
                true
            ).setExample("/replaychannel add #ergebnisse-emolga").build()
        }

        override suspend fun process(e: GuildCommandEvent) {
            val tco = e.textChannel
            val res = (e.arguments.getNullable<GuildChannel>("channel") ?: tco).takeIf { it is TextChannel } ?: run {
                e.reply("Du musst einen Textchannel angeben!")
                return
            }
            val l = AnalysisManager.insertChannel(tco, res.idLong)
            if (l == -1L) {
                e.reply(if (tco.idLong == res.idLong) "Dieser Channel ist nun ein Replaychannel, somit werden alle Replay-Ergebnisse automatisch hier reingeschickt!" else "Alle Ergebnisse der Replays aus ${tco.asMention} werden von nun an in den Channel ${res.asMention} geschickt!")
                replayAnalysis[tco.idLong] = res.idLong
            } else {
                e.reply("Die Replays aus diesem Channel werden ${if (l == res.idLong) "bereits" else "zurzeit"} in den Channel <#$l> geschickt! Mit /replaychannel remove kannst du dies ändern.")
            }
        }
    }

    class Remove : Command("remove", "Entfernt diesen Channel aus der Liste der Replaychannels") {
        init {
            argumentTemplate = ArgumentManagerTemplate.noArgs()
        }

        override suspend fun process(e: GuildCommandEvent) {
            val tco = e.textChannel
            if (AnalysisManager.deleteChannel(tco.idLong)) {
                e.reply("Dieser Channel ist kein Replaychannel mehr!")
                replayAnalysis.remove(tco.idLong)
            } else {
                e.reply("Dieser Channel ist zurzeit kein Replaychannel!")
            }
        }
    }

    override suspend fun process(e: GuildCommandEvent) {}
}
