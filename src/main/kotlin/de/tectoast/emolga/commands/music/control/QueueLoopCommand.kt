package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class QueueLoopCommand : MusicCommand("queueloop", "Loopt die derzeitige Queue oder beendet die Loop") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val manager = getGuildAudioPlayer(e.guild)
        e.reply("QueueLoop wurde " + (if (manager.scheduler.toggleQueueLoop()) "" else "de") + "aktiviert!")
    }
}