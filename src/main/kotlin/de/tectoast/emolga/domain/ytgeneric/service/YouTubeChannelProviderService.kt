package de.tectoast.emolga.domain.ytgeneric.service

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.ytgeneric.repository.YouTubeChannelsRepository
import de.tectoast.emolga.domain.ytgeneric.repository.YouTubeNotificationsRepository
import org.koin.core.annotation.Single

@Single
class YouTubeChannelProviderService(
    private val ytNotificationsRepo: YouTubeNotificationsRepository,
    private val ytChannelsRepo: YouTubeChannelsRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val leagueMemberRepo: LeagueMemberRepository
) {

    suspend fun getYouTubeChannelsThatShouldBeSubscribed(): Set<String> {
        val fromNotifications = ytNotificationsRepo.getAllYTChannels()
        val fromYouTubeLeague = ytChannelsRepo.getAllChannelIds(findYouTubeLeagueUsers())
        return fromNotifications + fromYouTubeLeague
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