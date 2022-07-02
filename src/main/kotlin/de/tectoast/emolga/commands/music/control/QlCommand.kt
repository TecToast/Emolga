package de.tectoast.emolga.commands.music.control

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class QlCommand : MusicCommand("ql", "Zeigt die Länge der Queue an") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val musicManager = getGuildAudioPlayer(tco.guild)
        val duration = musicManager.scheduler.queue.stream().mapToLong { obj: AudioTrack -> obj.duration }.sum() / 1000
        val hours = (duration / 3600).toInt()
        val minutes = ((duration - hours * 3600) / 60).toInt()
        val seconds = (duration - hours * 3600 - minutes * 60).toInt()
        var str = ""
        if (hours > 0) str += hours.toString() + "h "
        str += minutes.toString() + "m "
        str += seconds.toString() + "s"
        tco.sendMessage(str).queue()
    }
}