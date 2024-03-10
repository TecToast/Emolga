package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*

object Resend {
    val messageCache = mutableMapOf<Long, String>()

    object MessageContext : MessageContextFeature(MessageContextSpec("Resend")) {
        context(InteractionData) override suspend fun exec(e: MessageContextArgs) {
            messageCache[user] = e.message.contentRaw
            replyModal(Modal())
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("resend")) {
        override val title = "Resend"

        class Args : Arguments() {
            var tc by long("Channel", "Der Channel")
        }


        context(InteractionData)
        override suspend fun exec(e: Args) {
            jda.getTextChannelById(e.tc)!!.sendMessage(messageCache[user]!!).queue()
            done(true)
        }
    }
}
