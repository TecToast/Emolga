package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand

class FlegmonGehBitteCommand : PepeCommand("flegmongehbitte", "Sagt Flegmon, dass er bitte aus dem Voice gehen soll") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val am = e.guild.audioManager
        if (am.isConnected) {
            am.closeAudioConnection()
            e.reply("Dann gehe ich halt :c")
        } else {
            e.reply("Ich bin doch gar nicht da :c")
        }
    }
}