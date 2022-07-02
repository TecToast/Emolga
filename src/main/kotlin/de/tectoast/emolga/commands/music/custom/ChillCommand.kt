package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class ChillCommand : MusicCommand("chill", "Spielt die Chillplaylist ab", 712035338846994502L, 745934535748747364L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val member = e.member
        val g = tco.guild
        val musicManager = getGuildAudioPlayer(g)
        if (!chill.contains(g)) {
            val url = "https://www.youtube.com/playlist?list=PLPHBmr2YEhHS17xvYqjt0AgIReBuyAYc2"
            chill.add(g)
            loadPlaylist(tco, url, member, ":^)", true)
        } else {
            chill.remove(g)
            musicManager.player.stopTrack()
            musicManager.scheduler.queue.clear()
            tco.sendMessage(":^(").queue()
        }
    }
}