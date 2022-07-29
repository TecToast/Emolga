package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class DeepCommand : MusicCommand("deep", "Spielt die Deepplaylist ab", 700504340368064562L, 673833176036147210L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) =
        manageCustomPlaylist("https://www.youtube.com/playlist?list=PLaduIcpkVIbrBbU1vxkMSvKdOKo0GJx65", deep, e)
}