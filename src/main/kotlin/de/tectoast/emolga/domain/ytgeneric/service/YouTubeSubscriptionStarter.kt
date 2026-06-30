package de.tectoast.emolga.domain.ytgeneric.service

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.ytgeneric.repository.YouTubeChannelsRepository
import de.tectoast.emolga.domain.ytgeneric.repository.YouTubeNotificationsRepository
import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class YouTubeSubscriptionStarter(
    private val service: YouTubeSubscriptionService,
    private val ytNotificationsRepo: YouTubeNotificationsRepository,
    private val ytChannelsRepo: YouTubeChannelsRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    dispatcher: CoroutineDispatcher
) {
    private val scope = createCoroutineScope("YouTubeSubscriptionStarter", dispatcher)
    private val logger = KotlinLogging.logger {}
    fun setupYTSubscriptions() {
        scope.launch {
            val fromNotifications = ytNotificationsRepo.getAllYTChannels()
            val fromYouTubeLeague = ytChannelsRepo.getAllChannelIds(findYouTubeLeagueUsers())
            val allChannels = fromNotifications + fromYouTubeLeague
            logger.info("Subscribing to ${allChannels.size} channels...")
            allChannels.forEach {
                service.subscribeToChannel(it)
                delay(1.seconds)
            }
            logger.info("Done subscribing to ${allChannels.size} channels!")
        }
    }

    private suspend fun findYouTubeLeagueUsers() = buildSet {
        for (leagueName in leagueCoreRepo.getAllLeagueNames()) {
            val config = leagueConfigRepo.getConfig(leagueName)
            if (config.youtube != null) {
                addAll(leagueMemberRepo.getPrimaryIds(leagueName).flatMap { it.value })
            }
        }
    }


}