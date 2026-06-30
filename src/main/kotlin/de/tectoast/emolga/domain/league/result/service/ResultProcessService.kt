package de.tectoast.emolga.domain.league.result.service

import de.tectoast.emolga.domain.game.model.FullInputGame
import de.tectoast.emolga.domain.game.model.GameSource
import de.tectoast.emolga.domain.game.model.KDWithName
import de.tectoast.emolga.domain.game.model.SingleGame
import de.tectoast.emolga.domain.game.service.process.GameProcessService
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.result.repository.ResultCodesRepository
import de.tectoast.emolga.domain.league.util.model.LeagueResult
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import mu.KotlinLogging
import org.koin.core.annotation.Single

@Single
class ResultProcessService(
    private val repository: ResultCodesRepository,
    private val gameProcessService: GameProcessService,
    private val leagueConfigRepo: LeagueConfigRepository
) {
    private val logger = KotlinLogging.logger {}
    suspend fun processResult(resultid: String, body: List<List<List<KDWithName>>>): Unit? {
        if (!isValidBody(body)) return null
        val resData = repository.getEntryByCode(resultid) ?: return null
        val leagueResult = LeagueResult(resData.leagueName, listOf(resData.p1, resData.p2))
        val games = body.map { game ->
            SingleGame(
                source = GameSource.Direct(leagueResult),
                is4v4 = false,
                winnerIndex = game.indexOfFirst { it.sumOf { kd -> kd.deaths } < it.size },
                kd = game
            )
        }
        val fullInputGame = FullInputGame(games, false)
        val resultChannel = leagueConfigRepo.getConfig(resData.leagueName).resultChannel!!
        gameProcessService.analyseGame(
            fullInputGame = fullInputGame,
            infoSender = {},
            replaySender = {},
            resultchannelParam = resultChannel,
            guildOfChannel = resData.guild,
            errorSender = { logger.error(it.translateTo(K18N_DEFAULT_LANGUAGE)) }
        )
        return Unit
    }

    private fun isValidBody(body: List<List<List<KDWithName>>>): Boolean {
        runCatching {
            body.forEach { singleGame ->
                if (singleGame.size != 2 || singleGame.any { it.size > 6 }) return false
                for (i in 0..1) {
                    if (singleGame[i].any { it.deaths !in 0..1 }) return false
                    if (singleGame[i].sumOf { it.kills } != singleGame[1 - i].sumOf { it.deaths }) return false
                }
            }
        }.onFailure { return false }
        return true
    }
}