package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.GuildMetaRepository
import de.tectoast.emolga.discord.K18nMessageSender
import de.tectoast.emolga.discord.createSingleChannel
import de.tectoast.emolga.domain.game.model.analysis.ShowdownLogProvider
import de.tectoast.emolga.domain.game.service.process.FullInputGameBuilder
import de.tectoast.emolga.domain.game.service.process.GameProcessService
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class MultiReplayService(
    private val gameProcessService: GameProcessService,
    private val fullInputGameBuilder: FullInputGameBuilder,
    private val guildMetaRepo: GuildMetaRepository,
    private val channelInterface: ChannelInterface
) {
    suspend fun analyseMultiple(
        replayChannel: Long,
        resultChannel: Long,
        allReplays: List<String>,
        infoSender: K18nMessageSender
    ): CalcResult<Unit> {
        val guild = guildMetaRepo.getGuildFromChannelId(resultChannel)!!
        val lastIndex = allReplays.lastIndex
        val games = allReplays.map { url ->
            fullInputGameBuilder.fromShowdown(guild, listOf(ShowdownLogProvider.ReplayUrl(url)), infoSender)
                .getOrReturn { return it }
        }
        val replaySender = channelInterface.createSingleChannel(replayChannel)
        games.forEachIndexed { index, game ->
            gameProcessService.analyseGame(
                fullInputGame = game,
                infoSender = infoSender,
                replaySender = replaySender,
                resultchannelParam = resultChannel,
                guildOfChannel = guild,
                customGuild = guild,
                withSort = index == lastIndex
            )
        }
        return Unit.success()
    }
}
