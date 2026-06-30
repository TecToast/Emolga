package de.tectoast.emolga.domain.league.youtube.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.youtube.repository.YTVideoSendRepository
import org.koin.core.annotation.Single

@Single
class YouTubeSendService(
    private val leagueConfigRepo: LeagueConfigRepository,
    private val ytVideoSendRepo: YTVideoSendRepository,
    private val messageBuilder: YouTubeAnnouncementMessageBuilder,
    private val channelInterface: ChannelInterface
) {
    suspend fun send(leagueName: String, week: Int, battleIndex: Int, overrideEnabled: Boolean = false) {
        val ytConfig = leagueConfigRepo.getConfig(leagueName).youtube ?: return
        val ytVideoSaveData = ytVideoSendRepo.get(leagueName, week, battleIndex)
        if (!overrideEnabled && !ytVideoSaveData.enabled) return
        ytVideoSendRepo.disable(leagueName, week, battleIndex)
        val message =
            messageBuilder.buildMessage(leagueName, week, battleIndex, ytConfig.messageConfig, ytVideoSaveData.vids)
                ?: return
        channelInterface.sendMessage(ytConfig.sendChannel, message)
    }
}