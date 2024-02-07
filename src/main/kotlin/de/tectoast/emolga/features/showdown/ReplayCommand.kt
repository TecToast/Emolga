package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.database.exposed.AnalysisDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import mu.KotlinLogging

object ReplayCommand : CommandFeature<ReplayCommand.Args>(
    ::Args,
    CommandSpec("replay", "Analysiert ein Replay und schickt das Ergebnis in den konfigurierten Ergebnischannel", -1)
) {
    private val logger = KotlinLogging.logger {}

    init {
        registerPNListener { e ->
            val msg = e.message.contentDisplay
            if (msg.contains("https://") || msg.contains("http://")) {
                regex.find(msg)?.run {
                    val url = groupValues[0]
                    logger.info(url)
                    Command.analyseReplay(
                        url = url,
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

    val regex =
        Regex("https://replay\\.(?:ess\\.tectoast\\.de|pokemonshowdown\\.com)/(?:[a-z]+-)?([^-]+)-\\d+[-a-z0-9]*")

    context(InteractionData)
    override suspend fun exec(e: Args) {
        deferReply()
        val mr = regex.find(e.url) ?: return reply("Das ist kein gültiges Replay!")
        val channel = AnalysisDB.getResultChannel(tc)
            ?: return reply("Dieser Channel ist kein Replaychannel! Mit `/replaychannel add` kannst du diesen Channel zu einem Replaychannel machen!")
        val tc = jda.getTextChannelById(channel)
        if (tc == null) {
            reply("Ich habe keinen Zugriff auf den Ergebnischannel!")
            return
        }
        Command.analyseReplay(url = mr.groupValues[0], resultchannelParam = tc, fromReplayCommand = self)
    }
}
