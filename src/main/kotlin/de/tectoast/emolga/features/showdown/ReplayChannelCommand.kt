package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.database.exposed.AnalysisDB
import de.tectoast.emolga.features.*
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

object ReplayChannelCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("replaychannel", "Konfiguriert die Replay-Channel", -1)) {

    object Add : CommandFeature<Add.Args>(
        ::Args, CommandSpec("add", "Fügt einen Replaychannel hinzu, standardmäßig dieser Channel")
    ) {
        class Args : Arguments() {
            var channel by channel("Channel", "Der Channel, wo die Ergebnisse reingeschickt werden sollen (optional)") {
                validate { if (it is MessageChannel) it else throw InvalidArgumentException("Der Channel muss ein Text-Channel sein!") }
            }.nullable()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val tco = textChannel
            val res = e.channel ?: tco
            val l = AnalysisDB.insertChannel(tco.idLong, res.idLong, tco.guild.idLong)
            if (l == -1L) {
                reply(if (tco.idLong == res.idLong) "Dieser Channel ist nun ein Replaychannel, somit werden alle Replay-Ergebnisse automatisch hier reingeschickt!" else "Alle Ergebnisse der Replays aus ${tco.asMention} werden von nun an in den Channel ${res.asMention} geschickt!")
            } else {
                reply("Die Replays aus diesem Channel werden ${if (l == res.idLong) "bereits" else "zurzeit"} in den Channel <#$l> geschickt! Mit /replaychannel remove kannst du dies ändern.")
            }
        }
    }

    object Remove : CommandFeature<NoArgs>(NoArgs(), CommandSpec("remove", "Entfernt einen Replaychannel")) {
        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            val tco = textChannel
            if (AnalysisDB.deleteChannel(tco.idLong)) {
                reply("Dieser Channel ist kein Replaychannel mehr!")
            } else {
                reply("Dieser Channel ist zurzeit kein Replaychannel!")
            }
        }
    }

    context(InteractionData)
    override suspend fun exec(e: NoArgs) {

    }
}
