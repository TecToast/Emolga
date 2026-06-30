package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.BotConstants
import io.ktor.client.*
import net.dv8tion.jda.api.entities.User
import org.koin.core.annotation.Single

private const val EMOLGA_PN = 828044461379682314

@Single(binds = [ListenerProvider::class])
class DMRelay(private val httpClient: HttpClient, private val botConstants: BotConstants) : ListenerProvider() {

    init {
        registerDMListener { e ->
            if (!e.author.isBot && e.author.isNotBotOwner) e.jda.getTextChannelById(EMOLGA_PN)
                ?.sendMessage(e.author.asMention + ": " + e.message.contentDisplay)?.apply {
                    if (e.message.attachments.isNotEmpty()) addContent("\n\n" + e.message.attachments.joinToString("\n") { it.url })
                }?.queue()
        }
    }

    private inline val User.isNotBotOwner: Boolean get() = this.idLong != botConstants.botOwnerId
}
