package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.discord.K18nMessageSender
import de.tectoast.emolga.discord.MessageSender
import de.tectoast.emolga.domain.game.model.analysis.ShowdownLogProvider
import de.tectoast.emolga.domain.game.repository.ReplayChannelRepository
import de.tectoast.emolga.domain.game.service.process.FullInputGameBuilder
import de.tectoast.emolga.domain.game.service.process.GameProcessService
import de.tectoast.emolga.features.showdown.K18n_ReplayGeneric
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class ReplayService(
    private val replayChannelRepository: ReplayChannelRepository,
    private val gameProcessService: GameProcessService,
    private val fullInputGameBuilder: FullInputGameBuilder
) {
    suspend fun analyseReplay(
        guild: Long,
        tcId: Long,
        urlProvided: String,
        infoSender: K18nMessageSender,
        replaySender: MessageSender
    ): CalcResult<Unit> {
        val channel = replayChannelRepository.getResultChannel(tcId)
            ?: return K18n_ReplayGeneric.NoReplayChannel.error()
        val fullInputGame =
            fullInputGameBuilder.fromShowdown(guild, listOf(ShowdownLogProvider.ReplayUrl(urlProvided)), infoSender)
                .getOrReturn { return it }
        gameProcessService.analyseGame(
            fullInputGame = fullInputGame,
            infoSender = infoSender,
            replaySender = replaySender,
            resultchannelParam = channel,
            guildOfChannel = guild
        )
        return Unit.success()
    }

    suspend fun analyseBo3(
        guild: Long,
        tcId: Long,
        urlsProvided: List<String>,
        infoSender: K18nMessageSender,
        replaySender: MessageSender,
    ): CalcResult<Unit> {
        val channel = replayChannelRepository.getResultChannel(tcId)
            ?: return K18n_ReplayGeneric.NoReplayChannel.error()
        val fullInputGame =
            fullInputGameBuilder.fromShowdown(guild, urlsProvided.map { ShowdownLogProvider.ReplayUrl(it) }, infoSender)
                .getOrReturn { return it }
        gameProcessService.analyseGame(
            fullInputGame = fullInputGame,
            infoSender = infoSender,
            replaySender = replaySender,
            resultchannelParam = channel,
            guildOfChannel = guild
        )
        return Unit.success()
    }
}
