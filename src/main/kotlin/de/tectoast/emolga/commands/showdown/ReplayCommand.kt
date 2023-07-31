package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class ReplayCommand : Command(
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

    val regex = Regex("https://(replay\\.ess\\.tectoast\\.de|replay\\.pokemonshowdown\\.com)/([a-z]+-)?([^-]+)-\\d+")

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val url = args.getText("url")
        val slashEvent = e.slashCommandEvent!!
        slashEvent.deferReply().queue()
        val hook = slashEvent.hook
        val find = regex.find(url)
        val mr = find ?: return hook.sendMessage("Das ist kein gültiges Replay!").queue()
        val channel = replayAnalysis[e.textChannel.idLong]
            ?: return hook.sendMessage("Dieser Channel ist kein Replaychannel! Mit `/replaychannel add` kannst du diesen Channel zu einem Replaychannel machen!")
                .queue()
        val tc = e.jda.getTextChannelById(channel)
        if (tc == null) {
            hook.sendMessage("Ich habe keinen Zugriff auf den Ergebnischannel!").queue()
            return
        }
        analyseReplay(url = mr.groupValues[0], resultchannelParam = tc, fromReplayCommand = hook)
    }
}
