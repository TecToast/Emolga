package de.tectoast.emolga.utils

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.koin.core.annotation.Single

interface MessageSender {
    suspend fun sendMessage(message: String)
}

interface ChannelMessageSender {
    suspend fun sendMessage(channelId: Long, message: MessageCreateData): Long?
}
suspend fun ChannelMessageSender.sendMessage(channelId: Long, message: String, mentionUsers: List<Long>? = null): Long? {
    return sendMessage(channelId, MessageCreate {
        content = message
        mentionUsers?.let {
            mentions {
                users += it
            }
        }
    })
}

@Single
class JDAChannelMessageSender(private val jda: JDA) : ChannelMessageSender {
    override suspend fun sendMessage(channelId: Long, message: MessageCreateData): Long? {
        val channel = jda.getTextChannelById(channelId)
        return channel?.sendMessage(message)?.await()?.idLong
    }
}
