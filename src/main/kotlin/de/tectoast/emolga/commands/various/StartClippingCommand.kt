package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.CombinedAudio
import net.dv8tion.jda.api.entities.User
import org.apache.commons.collections4.queue.CircularFifoQueue

class StartClippingCommand :
    Command("startclipping", "Startet die Clip-Funktion c:", CommandCategory.Flo, 919639507740020846L) {
    override suspend fun process(e: GuildCommandEvent) {
        val am = e.guild.audioManager
        am.openAudioConnection(e.member.voiceState!!.channel)
        clips[e.guild.idLong] = CircularFifoQueue(1500)
        am.receivingHandler = object : AudioReceiveHandler {
            override fun canReceiveCombined(): Boolean {
                return true
            }

            override fun canReceiveUser(): Boolean {
                return false
            }

            override fun handleCombinedAudio(audio: CombinedAudio) {
                clips[e.guild.idLong]!!.add(audio.getAudioData(1.0))
            }

            override fun includeUserInCombinedAudio(user: User): Boolean {
                return true
            }
        }
        e.reply("Sehr interessant was ihr so redet \uD83D\uDC40")
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }
}