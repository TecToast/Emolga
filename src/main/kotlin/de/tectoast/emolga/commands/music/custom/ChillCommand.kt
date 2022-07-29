package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class ChillCommand : MusicCommand("chill", "Spielt die Chillplaylist ab", 712035338846994502L, 745934535748747364L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) =
        manageCustomPlaylist("https://www.youtube.com/playlist?list=PLPHBmr2YEhHS17xvYqjt0AgIReBuyAYc2", chill, e)
}