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

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val url = args.getText("url")
        val slashEvent = e.slashCommandEvent!!
        slashEvent.deferReply().queue()
        val hook = slashEvent.hook
        if (!url.startsWith("https://replay.pokemonshowdown.com/")) {
            hook.sendMessage("Das ist kein gültiges Replay!").queue()
            return
        }
        val channel = replayAnalysis[e.textChannel.idLong]
        if (channel == null) {
            hook.sendMessage("Dieser Channel ist kein Replaychannel! Mit `/replaychannel add` kannst du diesen Channel zu einem Replaychannel machen!")
                .queue()
            return
        }
        val tc = e.jda.getTextChannelById(channel)
        if (tc == null) {
            hook.sendMessage("Ich habe keinen Zugriff auf den Ergebnischannel!").queue()
            return
        }
        analyseReplay(url = url, resultchannel = tc, fromReplayCommand = hook)
    }
}
