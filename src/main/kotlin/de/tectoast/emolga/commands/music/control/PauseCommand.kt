package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class PauseCommand : MusicCommand("pause", "Pausiert den derzeitigen Track oder setzt ihn fort, wenn er pausiert ist") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val manager = getGuildAudioPlayer(e.guild)
        val player = manager.player
        if (player.playingTrack == null) {
            e.reply("Derzeit läuft kein Track!")
            return
        }
        player.isPaused = !player.isPaused
    }
}