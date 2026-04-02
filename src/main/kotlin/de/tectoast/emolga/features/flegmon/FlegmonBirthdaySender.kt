package de.tectoast.emolga.features.flegmon

import de.tectoast.emolga.utils.MessageSender
import net.dv8tion.jda.api.JDA
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
@Named("flegmonBirthdaySender")
class FlegmonBirthdaySender(
    @Named("flegmon") private val flegmonJda: JDA?
) : MessageSender {
    // The channel ID is currently hardcoded for the flegmon birthday sender
    private val channelId = 605650587329232896L

    override suspend fun sendMessage(message: String) {
        val channel = flegmonJda?.getTextChannelById(channelId)
        channel?.sendMessage(message)?.queue()
    }
}
