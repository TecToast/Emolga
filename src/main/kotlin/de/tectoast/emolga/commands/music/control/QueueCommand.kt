package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class QueueCommand : MusicCommand("q", "Zeigt die Queue an") {
    init {
        aliases.add("queue")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val g = tco.guild
        val musicManager = getGuildAudioPlayer(g)
        var str = StringBuilder()
        if (musicManager.player.playingTrack == null) {
            tco.sendMessage("Derzeit ist kein Lied in der Queue!").queue()
            return
        }
        val sched = musicManager.scheduler
        val queueLoop = sched.queueLoop
        if (queueLoop.size > 0) {
            var i = 1
            val num = queueLoop.size - sched.currQueueLoop.size
            for (audioTrack in queueLoop) {
                if (i == num) str.append("**")
                str.append(getWithZeros(i, 2))
                if (i == num) str.append("**")
                str.append(": `").append(audioTrack.info.title).append("`\n")
                if (str.length > 1900) {
                    tco.sendMessage(str.toString()).queue()
                    str = StringBuilder()
                }
                i++
            }
        } else {
            str.append("Gerade: `").append(musicManager.player.playingTrack.info.title).append("`\n")
            var i = 1
            for (audioTrack in sched.queue) {
                str.append(getWithZeros(i, 2)).append(": `").append(audioTrack.info.title).append("`\n")
                if (str.length > 1900) {
                    tco.sendMessage(str.toString()).queue()
                    str = StringBuilder()
                }
                i++
            }
        }
        tco.sendMessage(str.toString()).queue()
    }
}