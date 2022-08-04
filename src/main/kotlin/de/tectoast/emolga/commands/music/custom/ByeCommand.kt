package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class ByeCommand : MusicCommand("bye", ":^(", 700504340368064562L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) =
        loadAndPlay(e.textChannel, "https://www.youtube.com/watch?v=TgqiSBxvdws", e.member, ":(")

}