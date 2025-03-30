package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.ListenerProvider
import de.tectoast.emolga.utils.isNotFlo

object PNRelay : ListenerProvider() {
    private const val EMOLGA_PN = 828044461379682314

    init {
        registerDMListener { e ->
            if (!e.author.isBot && e.author.isNotFlo) e.jda.getTextChannelById(EMOLGA_PN)
                ?.sendMessage(e.author.asMention + ": " + e.message.contentDisplay)?.apply {
                    if (e.message.attachments.isNotEmpty()) addContent("\n\n" + e.message.attachments.joinToString("\n") { it.url })
                }?.queue()
        }
    }
}
