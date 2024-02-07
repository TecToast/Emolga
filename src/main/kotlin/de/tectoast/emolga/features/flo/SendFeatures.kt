package de.tectoast.emolga.features.flo

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import kotlin.math.min

object SendFeatures {
    class Args : Arguments() {
        var id by long("id", "id")
        var msg by string("msg", "msg")
    }

    object SendPNCommand : CommandFeature<Args>(::Args, CommandSpec("sendpn", "Sendet eine PN an einen User")) {
        init {
            restrict(flo)
        }

        context(InteractionData) override suspend fun exec(e: Args) {
            jda.openPrivateChannelById(e.id).flatMap { it.sendMessage(e.msg) }.queue()
            done(true)
        }

    }

    object SendTCCommand : CommandFeature<Args>(::Args, CommandSpec("sendtc", "Sendet eine Nachricht in einen TC")) {
        init {
            restrict(flo)
        }

        context(InteractionData) override suspend fun exec(e: Args) {
            jda.getTextChannelById(e.id)!!.sendMessage(e.msg).queue()
            done(true)
        }

    }

    fun sendToMe(msg: String) {
        sendToUser(Constants.FLOID, msg)
    }

    fun sendToUser(id: Long, msg: String) {
        val jda = jda
        jda.openPrivateChannelById(id).flatMap { pc ->
            pc.sendMessage(
                msg.substring(0, min(msg.length, 2000))
            )
        }.queue()
    }
}
