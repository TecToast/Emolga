package de.tectoast.emolga.discord.jda.flegmon

import de.tectoast.emolga.discord.MessageSender
import de.tectoast.emolga.discord.OptionalJDA
import de.tectoast.emolga.discord.jdaOrNull
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
@Named("flegmonBirthdaySender")
class FlegmonBirthdaySender(
    @Named("flegmon") private val flegmonJda: OptionalJDA
) : MessageSender {
    // The channel ID is currently hardcoded for the flegmon birthday sender
    private val channelId = 605650587329232896L

    override suspend fun sendMessage(message: MessageCreateData) {
        val channel = flegmonJda.jdaOrNull?.getTextChannelById(channelId)
        channel?.sendMessage(message)?.queue()
    }
}