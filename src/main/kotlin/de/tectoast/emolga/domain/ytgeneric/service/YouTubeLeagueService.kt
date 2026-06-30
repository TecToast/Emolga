package de.tectoast.emolga.domain.ytgeneric.service

import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleRepository
import de.tectoast.emolga.domain.league.util.service.LeagueQueryService
import de.tectoast.emolga.domain.league.youtube.repository.YTVideoSendRepository
import de.tectoast.emolga.domain.league.youtube.repository.YouTubeLeagueNamesRepository
import de.tectoast.emolga.domain.league.youtube.service.YouTubeSendService
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import de.tectoast.emolga.domain.ytgeneric.repository.YouTubeChannelsRepository
import mu.KotlinLogging
import org.koin.core.annotation.Single


@Single
class YouTubeLeagueService(
    private val ytLeagueNamesRepo: YouTubeLeagueNamesRepository,
    private val ytChannelsRepo: YouTubeChannelsRepository,
    private val leagueQueryService: LeagueQueryService,
    private val leagueScheduleRepo: LeagueScheduleRepository,
    private val ytVideoSendRepo: YTVideoSendRepository,
    private val ytSendService: YouTubeSendService,
    private val repeatTaskScheduler: RepeatTaskScheduler
) {
    private val logger = KotlinLogging.logger {}
    suspend fun handleIncoming(channelId: String, videoId: String, title: String) {
        for (guild in ytLeagueNamesRepo.getPossibleGuilds(title)) {
            handleVideo(channelId, videoId, guild)
        }
    }

    private suspend fun handleVideo(channelId: String, videoId: String, guild: Long) {
        logger.info("Handling video $videoId for channel $channelId in guild $guild")
        val uids = ytChannelsRepo.getUsersByChannelId(channelId)
        for (uid in uids) {
            val allLeagues = leagueQueryService.getByGuildUserWithoutConfig(guild, uid)
            for ((leagueName, idx) in allLeagues) {
                if (handleLeagueCheck(leagueName, idx, videoId)) return
            }
        }
    }

    private suspend fun handleLeagueCheck(leagueName: String, idx: Int, videoId: String): Boolean {
        val week =
            repeatTaskScheduler.getNumberOfToday(RepeatTaskType.RegisterInDoc(leagueName, battleIndex = 0))
                ?: return false
        val battleIndex = leagueScheduleRepo.getBattleIndex(leagueName, week, idx) ?: return false
        if (!ytVideoSendRepo.set(leagueName, week, battleIndex, idx, videoId)) {
            logger.warn("Failed to save video $videoId for $idx in $leagueName")
            return false
        }
        logger.info("Saving video $videoId for $idx in $leagueName")
        if (ytVideoSendRepo.bothVideosPresent(leagueName, week, battleIndex)) {
            ytSendService.send(leagueName, week, battleIndex)
        }
        return true
    }
}