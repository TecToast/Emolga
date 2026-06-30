package de.tectoast.emolga.domain.ytgeneric.service

import de.tectoast.emolga.domain.ytgeneric.model.ChannelIdResult
import de.tectoast.emolga.utils.Google
import org.koin.core.annotation.Single

@Single
class YouTubeChannelIdService(private val google: Google) {
    suspend fun mapToChannelId(channelId: String): ChannelIdResult {
        val base = channelId.substringBefore("?")
        val result =
            if ("@" !in base) base.substringAfter("channel/").takeIf { google.validateChannelIdExists(it) }
                ?.let { ChannelIdResult(channelId = it, handle = null) }
            else {
                val channelHandle = base.substringAfter("@")
                google.fetchChannelId(channelHandle)?.let { ChannelIdResult(channelId = it, handle = channelHandle) }
            }
        if (result == null) throw IllegalArgumentException("No channel found for $base")
        return result
    }
}