package de.tectoast.emolga.domain.ytgeneric.service

import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class YouTubeSubscriptionStarter(
    private val service: YouTubeSubscriptionService,
    private val ytChannelProvider: YouTubeChannelProviderService,
) {
    private val logger = KotlinLogging.logger {}
    suspend fun setupYTSubscriptions() {
        val allChannels = ytChannelProvider.getYouTubeChannelsThatShouldBeSubscribed()
        logger.info("Subscribing to ${allChannels.size} channels...")
        allChannels.forEach {
            service.subscribeToChannel(it)
            delay(1.seconds)
        }
        logger.info("Done subscribing to ${allChannels.size} channels!")
    }


}
