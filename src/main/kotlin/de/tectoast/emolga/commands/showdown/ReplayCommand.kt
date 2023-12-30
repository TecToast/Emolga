package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.database.exposed.AnalysisDB

object ReplayCommand : TestableCommand<ReplayCommandArgs>(
    "replay",
    "Analysiert ein Replay und schickt das Ergebnis in den konfigurierten Ergebnischannel",
    CommandCategory.Showdown
) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("url", "Replay-Link", "Der Replay-Link", ArgumentManagerTemplate.Text.any())
            setExample("/replay https://replay.pokemonshowdown.com/oumonotype-82345404")
        }
        slash(true, -1)
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = ReplayCommandArgs(e.arguments.getText("url"))

    context (InteractionData)
    override suspend fun exec(e: ReplayCommandArgs) {
        deferReply()
        val mr = regex.find(e.url) ?: return reply("Das ist kein gültiges Replay!")
        val channel = AnalysisDB.getResultChannel(tc)
            ?: return reply("Dieser Channel ist kein Replaychannel! Mit `/replaychannel add` kannst du diesen Channel zu einem Replaychannel machen!")
        val tc = jda.getTextChannelById(channel)
        if (tc == null) {
            reply("Ich habe keinen Zugriff auf den Ergebnischannel!")
            return
        }
        analyseReplay(url = mr.groupValues[0], resultchannelParam = tc, fromReplayCommand = self)
    }


    val regex =
            Regex("https://replay\\.(?:ess\\.tectoast\\.de|pokemonshowdown\\.com)/(?:[a-z]+-)?([^-]+)-\\d+[-a-z0-9]*")

}

class ReplayCommandArgs(val url: String) : CommandArgs
