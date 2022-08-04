package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class QlCommand : MusicCommand("ql", "Zeigt die Länge der Queue an") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val musicManager = getGuildAudioPlayer(tco.guild)
        val duration = musicManager.scheduler.queue.sumOf { it.duration } / 1000
        val hours = (duration / 3600).toInt()
        val minutes = ((duration - hours * 3600) / 60).toInt()
        val seconds = (duration - hours * 3600 - minutes * 60).toInt()
        e.reply(buildString {
            if (hours > 0) append(hours).append("h ")
            append(minutes).append("m ")
            append(seconds).append("s ")
        })
    }
}