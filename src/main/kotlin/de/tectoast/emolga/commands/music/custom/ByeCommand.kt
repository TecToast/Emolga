package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class ByeCommand : MusicCommand("bye", ":^(", 700504340368064562L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val member = e.member
        try {
            loadAndPlay(tco, "https://www.youtube.com/watch?v=TgqiSBxvdws", member, ":(")
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
    }
}