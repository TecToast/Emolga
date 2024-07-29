package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.database.exposed.AnalysisDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.showdown.Analysis
import dev.minn.jda.ktx.generics.getChannel
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

object ReplayCommand : CommandFeature<ReplayCommand.Args>(
    ::Args,
    CommandSpec("replay", "Analysiert ein Replay und schickt das Ergebnis in den konfigurierten Ergebnischannel", -1)
) {
    private val logger = KotlinLogging.logger {}

    init {
        registerPNListener { e ->
            val msg = e.message.contentDisplay
            if (msg.contains("https://") || msg.contains("http://")) {
                Analysis.regex.find(msg)?.run {
                    val url = groupValues[0]
                    logger.info(url)
                    Analysis.analyseReplay(
                        urlProvided = url,
                        //customReplayChannel = e.jda.getTextChannelById(999779545316069396),
                        resultchannelParam = e.jda.getTextChannelById(820359155612254258)!!, message = e.message
                    )
                }
            }
        }
    }

    class Args : Arguments() {
        var url by string("Replay-Link", "Der Replay-Link")
    }



    context(InteractionData)
    override suspend fun exec(e: Args) {
        deferReply()
        val channel = AnalysisDB.getResultChannel(tc)
            ?: return reply("Dieser Channel ist kein Replaychannel! Mit `/replaychannel add` kannst du diesen Channel zu einem Replaychannel machen!")
        val tc = jda.getChannel<GuildMessageChannel>(channel)
        if (tc == null) {
            reply("Ich habe keinen Zugriff auf den Ergebnischannel!")
            return
        }
        Analysis.analyseReplay(urlProvided = e.url, resultchannelParam = tc, fromReplayCommand = self)
    }
}
