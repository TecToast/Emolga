package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class GSMusicCommand : MusicCommand("gsmusic", "Spielt die GamerSquad Playlist ab", 673833176036147210L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) =
        manageCustomPlaylist("https://www.youtube.com/playlist?list=PLrwrdAXSpHC5Mr2zC-q_dWKONVybk6JO6", music, e)
}