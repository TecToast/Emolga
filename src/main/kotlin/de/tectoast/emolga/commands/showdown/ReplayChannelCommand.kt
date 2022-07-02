package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.AnalysisManager
import net.dv8tion.jda.api.entities.TextChannel

class ReplayChannelCommand : Command(
    "replaychannel",
    "Schickt von nun an die Ergebnisse aller Replays, die in diesen Channel geschickt werden, in den angegebenen Channel (wenn die Ergebnisse in den gleichen Channel sollen, tagge einfach diesen Channel hier)",
    CommandCategory.Showdown
) {
    init {
        everywhere = true
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "action", "Aktion", "Die Aktion, die du durchführen möchtest",
                ArgumentManagerTemplate.Text.of(
                    SubCommand.of(
                        "add",
                        "Fügt einen Channel hinzu (Standard, falls dieses Argument weggelassen wird)"
                    ), SubCommand.of("remove", "Removed einen Channel")
                ), true
            )
            .add(
                "channel",
                "Channel",
                "Der Channel, wo die Ergebnisse reingeschickt werden sollen",
                ArgumentManagerTemplate.DiscordType.CHANNEL,
                true
            )
            .setExample("!replaychannel #ergebnisse-emolga")
            .build()
        aliases.add("replay")
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val m = e.message!!
        if (e.usedName == "replay") {
            sendToUser(
                e.author,
                "Der Command wurde in !replaychannel umbenannt, damit er sich nicht mehr mit anderen Bots schneidet. !replay funktioniert weiterhin, jedoch sollte am besten !replaychannel verwendet werden."
            )
        }
        val channels = m.mentions.getChannels(TextChannel::class.java)
        val sameChannel = channels.size == 0
        val tc = if (sameChannel) tco else channels[0]
        val args = e.arguments!!
        if (args.has("action") && args.isText("action", "remove")) {
            if (AnalysisManager.deleteChannel(tco.idLong)) {
                e.reply("Dieser Channel ist nun kein Replaychannel mehr!")
                replayAnalysis.remove(tco.idLong)
            } else {
                e.reply("Dieser Channel ist zurzeit kein Replaychannel!")
            }
        } else {
            //Database.insert("analysis", "replay, result", tco.getIdLong(), tc.getIdLong());
            val l = AnalysisManager.insertChannel(tco, tc)
            if (l == -1L) {
                e.reply(if (sameChannel) "Dieser Channel ist nun ein Replaychannel, somit werden alle Replay-Ergebnisse automatisch hier reingeschickt!" else "Alle Ergebnisse der Replays aus " + tco.asMention + " werden von nun an in den Channel " + tc.asMention + " geschickt!")
                replayAnalysis[tco.idLong] = tc.idLong
            } else {
                e.reply("Die Replays aus diesem Channel werden " + (if (l == tc.idLong) "bereits" else "zurzeit") + " in den Channel <#" + l + "> geschickt!")
            }
        }
    }
}