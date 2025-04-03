package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.database.exposed.AnalysisDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.l
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

object ReplayChannelCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("replaychannel", "Konfiguriert die Replay-Channel", -1)) {

    object Add : CommandFeature<Add.Args>(
        ::Args,
        CommandSpec("add", "Fügt einen Replaychannel hinzu, standardmäßig ist dieser Channel der Ergebnis-Channel")
    ) {
        class Args : Arguments() {
            var channel by channel("Channel", "Der Channel, wo die Ergebnisse reingeschickt werden sollen (optional)") {
                validate { if (it is MessageChannel) it else throw InvalidArgumentException("Der Channel muss ein Text-Channel sein!") }
            }.nullable()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val resultChannel = e.channel?.idLong ?: tc
            val result = AnalysisDB.insertChannel(tc, resultChannel, gid)
            reply(
                when (result) {
                    AnalysisDB.AnalysisResult.CREATED -> {
                        if (tc == resultChannel) "Dieser Channel ist nun ein Replaychannel, somit werden alle Replay-Ergebnisse automatisch hier reingeschickt!"
                        else "Alle Ergebnisse der Replays aus <#${tc}> werden von nun an in den Channel <#${resultChannel}> geschickt!"
                    }

                    is AnalysisDB.AnalysisResult.Existed -> {
                        "Die Replays aus diesem Channel werden ${if (result.channel == resultChannel) "bereits" else "zurzeit"} in den Channel <#$l> geschickt! Mit /replaychannel remove kannst du dies ändern."
                    }
                }
            )
        }
    }

    object Remove : CommandFeature<NoArgs>(NoArgs(), CommandSpec("remove", "Entfernt einen Replaychannel")) {
        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            if (AnalysisDB.deleteChannel(tc)) {
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
