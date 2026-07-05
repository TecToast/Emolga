package de.tectoast.emolga.domain.league.prediction.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.prediction.model.config.PredictionGameLeaderboardConfig
import de.tectoast.emolga.utils.getOrReturn
import org.koin.core.annotation.Single

@Single
class PredictionGameLeaderboardService(
    private val predictionGameAnalyseTextService: PredictionGameAnalyseTextService,
    private val channelInterface: ChannelInterface,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val languageRepo: GuildConfigRepository
) {
    suspend fun sendNewLeaderboard(leagueName: String, config: PredictionGameLeaderboardConfig) {
        val guild = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName)?.guild ?: return
        val message =
            predictionGameAnalyseTextService.getTopNOfGuild(guild, config.topN).getOrReturn<_, Unit> { return }
        val language = languageRepo.getLanguage(guild)
        channelInterface.sendMessage(config.channel, message.translateTo(language))
    }
}