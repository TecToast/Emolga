package de.tectoast.emolga.commands

import net.dv8tion.jda.api.entities.Guild

abstract class MusicCommand(name: String, help: String, vararg guildIds: Long) :
    Command(name, help, CommandCategory.Music, *guildIds) {
    init {
        otherPrefix = true
        addCustomChannel(712035338846994502L, 716221567079546983L, 735076688144105493L)
    }

    fun manageCustomPlaylist(url: String, list: MutableList<Guild>, e: GuildCommandEvent) {
        val g = e.guild
        val musicManager = getGuildAudioPlayer(g)
        if (!list.contains(g)) {
            list.add(g)
            loadPlaylist(e.textChannel, url, e.member, ":^)", true)
        } else {
            list.remove(g)
            musicManager.player.stopTrack()
            musicManager.scheduler.queue.clear()
            e.reply(":^(")
        }
    }
}