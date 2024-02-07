package de.tectoast.emolga.features.flo

import de.tectoast.emolga.commands.isNotFlo
import de.tectoast.emolga.features.ListenerProvider

object PNRelay : ListenerProvider() {
    private const val EMOLGA_PN = 828044461379682314

    init {
        registerPNListener { e ->
            if (e.author.isBot && e.author.isNotFlo) e.jda.getTextChannelById(EMOLGA_PN)
                ?.sendMessage(e.author.asMention + ": " + e.message.contentDisplay)?.apply {
                    if (e.message.attachments.isNotEmpty()) addContent("\n\n" + e.message.attachments.joinToString("\n") { it.url })
                }?.queue()
        }
    }
}
