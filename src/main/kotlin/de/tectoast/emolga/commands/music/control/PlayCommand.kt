package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class PlayCommand : MusicCommand("p", "Fügt das Lied der Queue hinzu") {
    init {
        aliases.add("play")
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "video",
                "Link o Suchbegriff",
                "Der Link bzw. der Suchbegriff für ein YouTube-Video",
                ArgumentManagerTemplate.Text.any()
            )
            .setExample("e!p https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        try {
            loadAndPlay(e.textChannel, e.arguments!!.getText("video"), e.member, null)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
    }
}