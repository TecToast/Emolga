package de.tectoast.emolga.utils.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

class GuildMusicManager(manager: AudioPlayerManager) {
    @JvmField
    val player: AudioPlayer

    /**
     * Track scheduler for the player.
     */
    @JvmField
    val scheduler: TrackScheduler

    init {
        player = manager.createPlayer()
        scheduler = TrackScheduler(player)
        player.addListener(scheduler)
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    val sendHandler: AudioPlayerSendHandler
        get() = AudioPlayerSendHandler(player)
}