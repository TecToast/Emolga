package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class SkipCommand : MusicCommand("s", "Skippt den derzeitigen Track") {
    init {
        aliases.add("skip")
        addCustomChannel(712035338846994502L, 716221567079546983L, 735076688144105493L)
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        skipTrack(e.textChannel)
    }
}