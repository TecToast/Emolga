package de.tectoast.emolga.domain.ytgeneric.service

import org.koin.core.annotation.Single

@Single
class YouTubeSubscriptionValidationService(
    private val ytChannelProvider: YouTubeChannelProviderService,
) {
    suspend fun handleChallengeVerification(mode: String?, topic: String?, challenge: String?): String? {
        if (mode != "subscribe") return null
        if (topic == null || topic.substringAfter(
                "https://www.youtube.com/xml/feeds/videos.xml?channel_id=",
                missingDelimiterValue = ""
            ) !in ytChannelProvider.getYouTubeChannelsThatShouldBeSubscribed()
        ) return null
        return challenge
    }
}