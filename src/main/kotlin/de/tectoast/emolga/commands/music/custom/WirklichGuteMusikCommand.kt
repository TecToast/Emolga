package de.tectoast.emolga.commands.music.custom

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand
import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel

class WirklichGuteMusikCommand :
    MusicCommand("gutemusik", "Wirklich Gute Musik (Empfohlen von Flo und Dasor :) )", Constants.FPLID) {
    override fun process(e: GuildCommandEvent) {
        doIt(e.textChannel, e.member, true)
    }

    companion object {
        @JvmStatic
        fun doIt(tc: TextChannel, mem: Member, good: Boolean) {
            try {
                loadAndPlay(
                    tc,
                    if (good) "https://www.youtube.com/watch?v=4Diu2N8TGKA" else "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    mem,
                    "**ITS GUTE MUSIK TIME!**"
                )
                getGuildAudioPlayer(tc.guild).scheduler.enableLoop()
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
            }
        }
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }
}