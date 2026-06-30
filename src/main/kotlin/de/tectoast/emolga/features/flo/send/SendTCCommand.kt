package de.tectoast.emolga.features.flo.send

import de.tectoast.emolga.discord.GeneralMessageSender
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SendTCCommand(private val sender: GeneralMessageSender) :
    CommandFeature<SendArgs>(::SendArgs, CommandSpec("sendtc", "Sendet eine Nachricht in einen TC".k18n)) {
    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: SendArgs) {
        sender.sendToChannel(e.id, e.msg.convertForSend())
        iData.done(true)
    }

}