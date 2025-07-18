package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

object Resend {
    val messageCache = mutableMapOf<Long, Message>()

    object MessageContext : MessageContextFeature(MessageContextSpec("Resend")) {
        context(iData: InteractionData) override suspend fun exec(e: MessageContextArgs) {
            messageCache[iData.user] = e.message
            iData.replyModal(Modal())
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("resend")) {
        override val title = "Resend"

        class Args : Arguments() {
            var tc by long("Channel", "Der Channel")
        }


        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.jda.getTextChannelById(e.tc)!!
                .sendMessage(MessageCreateBuilder().applyMessage(messageCache[iData.user]!!).build()).queue()
            iData.done(true)
        }
    }
}
