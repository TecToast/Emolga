package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class LoopCommand : MusicCommand("loop", "Loopt den derzeitigen Track oder beendet die Loop") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        e.reply("Loop wurde ${if (getGuildAudioPlayer(e.guild).scheduler.toggleLoop()) "" else "de"}aktiviert!")
    }
}