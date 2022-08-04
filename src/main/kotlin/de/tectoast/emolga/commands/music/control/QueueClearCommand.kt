package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class QueueClearCommand : MusicCommand("c", "Cleart die Queue") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val musicManager = getGuildAudioPlayer(tco.guild)
        musicManager.scheduler.queue.clear()
        tco.sendMessage("Die Queue wurde geleert!").queue()
    }
}