package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class GoinCommand : Command("goin", "Spielt das YT-Video im Voice ab", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("vid", "Voicechannel-ID", "Die ID des Voicechannels", ArgumentManagerTemplate.DiscordType.ID)
            .add("link", "Link", "Der Link zum YT-Video", ArgumentManagerTemplate.Text.any())
            .setExample("!goin 744911735705829386 https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        loadAndPlay(e.textChannel, args.getText("link"), EmolgaMain.emolgajda.getVoiceChannelById(args.getID("vid"))!!)
    }
}