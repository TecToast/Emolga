package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class DcCommand : MusicCommand("dc", "Lässt den Bot disconnecten") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val m = e.message!!
        val msg = m.contentDisplay
        if (tco.guild.id == "447357526997073930") {
            e.jda.getGuildById(msg.substring(4))!!.audioManager.closeAudioConnection()
            return
        }
        tco.guild.audioManager.closeAudioConnection()
    }
}