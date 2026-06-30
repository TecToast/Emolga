package de.tectoast.emolga.features.flo.send

import de.tectoast.emolga.discord.GeneralMessageSender
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SendPNCommand(private val sender: GeneralMessageSender) :
    CommandFeature<SendArgs>(::SendArgs, CommandSpec("sendpn", "Sendet eine PN an einen User".k18n)) {
    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: SendArgs) {
        val msg = e.msg.convertForSend()
        sender.sendToUser(e.id, msg)
        iData.replyRaw("-> <@${e.id}>: $msg")
    }

}