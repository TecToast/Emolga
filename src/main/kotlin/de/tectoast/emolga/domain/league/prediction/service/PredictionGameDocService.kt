package de.tectoast.emolga.domain.league.prediction.service

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.prediction.model.config.PredictionGameDocConfig
import de.tectoast.emolga.domain.league.prediction.repository.PredictionGameVoteRepository
import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import de.tectoast.emolga.utils.dsl.Coord
import de.tectoast.emolga.utils.sheetupdate.SpreadsheetService
import org.koin.core.annotation.Single

@Single
class PredictionGameDocService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val userService: DiscordUserService,
    private val spreadsheetService: SpreadsheetService,
    private val votesRepo: PredictionGameVoteRepository
) {

    suspend fun executeUpUntil(leagueName: String, maxWeek: Int, config: PredictionGameDocConfig) {
        val guild = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName)?.guild ?: return
        val votes =
            (1..maxWeek).flatMap { week -> votesRepo.getAllPredictionGameVotesForWeek(guild, week).map { week to it } }
        val userData = userService.getData(guild, votes.mapTo(mutableSetOf()) { it.second.userId })
        spreadsheetService.updateSheet(config.sheetId, wait = false) {
            addAll(Coord(config.sheet, config.x, config.y), votes.map { (week, vote) ->
                listOf(
                    week,
                    userData[vote.userId]?.displayName ?: vote.userId.toString(),
                    vote.leagueName,
                    vote.battle + 1,
                    vote.idx
                )
            })
        }
    }


}
