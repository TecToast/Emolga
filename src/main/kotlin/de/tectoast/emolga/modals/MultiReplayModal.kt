package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.Command.Companion.analyseReplay
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

object MultiReplayModal : ModalListener("multireplay") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val urls = e.getValue("urls")!!.asString
        val (replay, result) = name!!.split(":").map { it.toLong() }
        val replayChannel = e.jda.getTextChannelById(replay)!!
        val resultChannel = e.jda.getTextChannelById(result)!!
        val allReplays = urls.split("\n")
        val lastIndex = allReplays.lastIndex

        e.reply("Replays werden analysiert!").setEphemeral(true).queue()
        allReplays.forEachIndexed { index, url ->
            analyseReplay(
                url = url,
                customReplayChannel = replayChannel,
                resultchannelParam = resultChannel,
                customGuild = replayChannel.guild.idLong,
                withSort = index == lastIndex
            )
        }
    }
}
