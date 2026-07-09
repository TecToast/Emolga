package de.tectoast.emolga.domain.ytgeneric.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.DMSender
import de.tectoast.emolga.discord.sendDM
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.ytgeneric.repository.YouTubeNotificationsRepository
import org.koin.core.annotation.Single
import java.util.*

@Single
class YouTubeNotificationService(
    private val ytNotificationsRepo: YouTubeNotificationsRepository,
    private val channelInterface: ChannelInterface,
    private val dmSender: DMSender
) {
    private val duplicateVideoCache = Collections.synchronizedSet<String>(mutableSetOf())

    suspend fun handleIncoming(channelId: String, videoId: String) {
        if (!duplicateVideoCache.add(videoId)) return
        val ytLink = "https://youtu.be/$videoId"
        ytNotificationsRepo.getDCChannels(channelId).forEach { (mc, dm, format) ->
            val content = format.replace("{ytlink}", ytLink)
            if (dm) dmSender.sendDM(mc, content)
            else channelInterface.sendMessage(mc, content)
        }
    }
}
