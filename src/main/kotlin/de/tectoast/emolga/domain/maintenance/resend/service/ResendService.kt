package de.tectoast.emolga.domain.maintenance.resend.service

import net.dv8tion.jda.api.entities.Message
import org.koin.core.annotation.Single

@Single
class ResendService {
    private val messageCache = mutableMapOf<Long, Message>()

    fun setMessage(userId: Long, message: Message) {
        messageCache[userId] = message
    }

    fun getMessage(userId: Long): Message? {
        return messageCache[userId]
    }
}