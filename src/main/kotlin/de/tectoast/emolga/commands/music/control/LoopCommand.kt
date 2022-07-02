package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class LoopCommand : MusicCommand("loop", "Loopt den derzeitigen Track oder beendet die Loop") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val manager = getGuildAudioPlayer(e.guild)
        e.reply("Loop wurde " + (if (manager.scheduler.toggleLoop()) "" else "de") + "aktiviert!")
    }
}