package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.discord.K18nMessageSender
import de.tectoast.emolga.discord.MessageSender
import de.tectoast.emolga.domain.game.model.analysis.ShowdownLogProvider
import de.tectoast.emolga.domain.game.service.process.FullInputGameBuilder
import de.tectoast.emolga.domain.game.service.process.GameProcessService
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class ReplayWithGuildService(
    private val fullInputGameBuilder: FullInputGameBuilder,
    private val gameProcessService: GameProcessService
) {
    suspend fun analyseForGuild(
        urls: List<String>,
        guild: Long,
        customGuild: Long,
        tcId: Long,
        infoSender: K18nMessageSender,
        replaySender: MessageSender
    ): CalcResult<Unit> {
        val fullInputGame = fullInputGameBuilder.fromShowdown(customGuild, urls.map {
            ShowdownLogProvider.ReplayUrl(it)
        }, infoSender).getOrReturn { return it }
        gameProcessService.analyseGame(
            fullInputGame = fullInputGame,
            infoSender = infoSender,
            replaySender = replaySender,
            resultchannelParam = tcId,
            guildOfChannel = guild,
            customGuild = customGuild,
        )
        return Unit.success()
    }
}
