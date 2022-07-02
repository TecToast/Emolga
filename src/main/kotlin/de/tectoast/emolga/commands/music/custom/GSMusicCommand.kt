package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class GSMusicCommand : MusicCommand("gsmusic", "Spielt die GamerSquad Playlist ab", 673833176036147210L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val member = e.member
        val g = tco.guild
        val musicManager = getGuildAudioPlayer(g)
        if (!music.contains(g)) {
            val url = "https://www.youtube.com/playlist?list=PLrwrdAXSpHC5Mr2zC-q_dWKONVybk6JO6"
            music.add(g)
            loadPlaylist(tco, url, member, ":^)")
        } else {
            music.remove(g)
            musicManager.player.stopTrack()
            musicManager.scheduler.queue.clear()
            tco.sendMessage(":^(").queue()
        }
    }
}