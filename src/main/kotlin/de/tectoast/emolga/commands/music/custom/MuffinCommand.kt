package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand
import de.tectoast.emolga.utils.Constants

class MuffinCommand : MusicCommand("muffin", "`!muffin` ITS MUFFIN TIME!", Constants.BSID, Constants.CULTID) {
    override fun process(e: GuildCommandEvent) =
        loadAndPlay(e.textChannel, "https://www.youtube.com/watch?v=LACbVhgtx9I", e.member, "**ITS MUFFIN TIME!**")

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }
}