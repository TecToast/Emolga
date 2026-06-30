package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.discord.ChannelPresenceChecker
import de.tectoast.emolga.domain.game.repository.ReplayChannelRepository
import org.koin.core.annotation.Single

@Single
class ReplayChannelService(
    private val repo: ReplayChannelRepository,
    private val channelChecker: ChannelPresenceChecker
) {
    suspend fun cleanupUnusedChannels(): Int {
        val allChannels = repo.getAllReplayChannels()
        val unusedChannels = allChannels.filterNot { channelId ->
            channelChecker.doesChannelExist(channelId)
        }
        return repo.deleteChannels(unusedChannels)
    }
}