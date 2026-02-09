package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.k18n
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

object Resend {
    val messageCache = mutableMapOf<Long, Message>()

    object MessageContext : MessageContextFeature(MessageContextSpec("Resend")) {
        context(iData: InteractionData)
        override suspend fun exec(e: MessageContextArgs) {
            messageCache[iData.user] = e.message
            iData.replyModal(Modal())
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("resend")) {
        override val title = "Resend".k18n

        class Args : Arguments() {
            var tc by long("Channel", "Der Channel".k18n)
        }


        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.jda.getTextChannelById(e.tc)!!
                .sendMessage(MessageCreateBuilder().applyMessage(messageCache[iData.user]!!).build()).queue()
            iData.done(true)
        }
    }
}
