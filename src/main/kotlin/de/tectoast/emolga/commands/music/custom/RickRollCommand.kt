package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class RickRollCommand : MusicCommand("rickroll", ":^)") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) =
        loadAndPlay(
            e.textChannel,
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            e.member,
            null
        ).also { e.reply(":^)") }
}