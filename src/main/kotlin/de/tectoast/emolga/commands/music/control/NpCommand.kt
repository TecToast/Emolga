package de.tectoast.emolga.commands.music.control

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.MusicCommand

class NpCommand : MusicCommand("np", "Zeigt, welcher Track gerade läuft") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val manager = getGuildAudioPlayer(tco.guild)
        val player = manager.player
        if (player.playingTrack == null) {
            tco.sendMessage("Derzeit läuft kein Track!").queue()
            return
        }
        val t = player.playingTrack
        val p = t.position
        val cm = (p / 60000).toInt()
        val cs = ((p - cm * 60000) / 1000).toInt()
        val d = t.duration
        val dm = (d / 60000).toInt()
        val ds = ((d - dm * 60000) / 1000).toInt()
        tco.sendMessage(
            "`${t.info.title}`\n${getWithZeros(cm, 2)}:${getWithZeros(cs, 2)} / ${
                getWithZeros(dm, 2)
            }:${getWithZeros(ds, 2)}"
        ).queue()
    }
}