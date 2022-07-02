package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class DeepCommand : MusicCommand("deep", "Spielt die Deepplaylist ab", 700504340368064562L, 673833176036147210L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val member = e.member
        val g = tco.guild
        val musicManager = getGuildAudioPlayer(g)
        if (!deep.contains(g)) {
            val url = "https://www.youtube.com/playlist?list=PLaduIcpkVIbrBbU1vxkMSvKdOKo0GJx65"
            deep.add(g)
            loadPlaylist(tco, url, member, ":^)", true)
        } else {
            deep.remove(g)
            musicManager.player.stopTrack()
            musicManager.scheduler.queue.clear()
            tco.sendMessage(":^(").queue()
        }
    }
}