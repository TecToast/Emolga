package de.tectoast.emolga.domain.game.service.process

import de.tectoast.emolga.discord.K18nMessageSender
import de.tectoast.emolga.domain.discord.service.GeneralDiscordService
import de.tectoast.emolga.domain.game.model.FullInputGame
import de.tectoast.emolga.domain.game.model.GameSource
import de.tectoast.emolga.domain.game.model.KDWithName
import de.tectoast.emolga.domain.game.model.SingleGame
import de.tectoast.emolga.domain.game.model.analysis.*
import de.tectoast.emolga.domain.game.service.process.analysis.AnalysisService
import de.tectoast.emolga.domain.game.service.process.analysis.BattleContext
import de.tectoast.emolga.domain.game.service.process.analysis.SDPlayer
import de.tectoast.emolga.domain.game.service.process.analysis.SDPokemon
import de.tectoast.emolga.domain.league.showdownnames.repository.SDNamesRepository
import de.tectoast.emolga.domain.league.util.service.LeagueQueryService
import de.tectoast.emolga.domain.statistics.repository.StatisticsRepository
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.showdown.K18n_Analysis
import de.tectoast.emolga.utils.success
import de.tectoast.emolga.utils.toShowdownUserId
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import mu.KotlinLogging
import org.koin.core.annotation.Single

@Single
class FullInputGameBuilder(
    private val analysisService: AnalysisService,
    private val sdNamesRepo: SDNamesRepository,
    private val leagueQueryService: LeagueQueryService,
    private val statisticsRepository: StatisticsRepository,
    private val generalDiscordService: GeneralDiscordService,
    baseScope: CoroutineScope
) {

    private val scope = baseScope + CoroutineName("Analysis")
    private val logger = KotlinLogging.logger {}

    suspend fun fromShowdown(
        guildId: Long, logProviders: List<ShowdownLogProvider>, infoSender: K18nMessageSender
    ): CalcResult<FullInputGame> {
        var dontTranslateFromReplayServer = false
        val singleGames = logProviders.map { logProvider ->
            val data = try {
                when (logProvider) {
                    is ShowdownLogProvider.ReplayUrl -> analysisService.analyse(
                        logProvider.url,
                        infoSender::sendMessage
                    )

                    is ShowdownLogProvider.ReplayLog -> analysisService.analyseFromLog(logProvider.log, "FILE")
                }
            } catch (ex: Exception) {
                return when (ex) {
                    is ShowdownDoesNotAnswerException -> K18n_Analysis.ErrorShowdownDoesNotAnswer
                    is ShowdownDoesntExistException -> K18n_Analysis.ErrorShowdownDoesntExist
                    is ShowdownParseException -> K18n_Analysis.ErrorShowdownParse
                    is InvalidReplayException -> K18n_Analysis.ErrorInvalidReplay
                    else -> {
                        logger.error(
                            "Error on replay analysis: $logProvider", ex
                        )
                        K18n_Analysis.ErrorGeneric
                    }
                }.error()
            }
            val (game, ctx, dontTranslate) = data
            dontTranslateFromReplayServer = dontTranslateFromReplayServer || dontTranslate
            addToStatistics(ctx)
            val uids = sdNamesRepo.getIDsByUsernames(game.map { it.nickname.toShowdownUserId() })
            logger.info("Analysed!")
            game.forEach { player ->
                player.pokemon.addAll(List((player.teamSize - player.pokemon.size).coerceAtLeast(0)) {
                    SDPokemon("_???_", -1)
                })
            }
            val leagueResult = leagueQueryService.leagueByShowdownReplay(guildId, game, ctx, uids)
            SingleGame(
                source = GameSource.FromReplay(leagueResult, ctx.url, game.map { it.nickname }, ctx.format),
                is4v4 = ctx.is4v4,
                winnerIndex = game.indexOfFirst { it.winnerOfGame },
                kd = game.toKDWithName(),
                defaultNameLookup = game.toDefaultNameLookup()
            )
        }
        return FullInputGame(singleGames, dontTranslateFromReplayServer).success()
    }


    private fun List<SDPlayer>.toKDWithName() =
        map { p -> p.pokemon.map { KDWithName(it.showdownIDInRoster, it.kills, it.deadCount) } }

    private fun List<SDPlayer>.toDefaultNameLookup() =
        flatMap { it.pokemon }.associate { it.showdownIDInRoster to it.pokemon }

    private fun addToStatistics(ctx: BattleContext) {
        scope.launch {
            statisticsRepository.addToStatistics(ctx)
            generalDiscordService.updatePresence()
        }
    }

}