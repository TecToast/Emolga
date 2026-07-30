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
    suspend fun execute(leagueName: String, week: Int, config: PredictionGameDocConfig) {
        val guild = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName)?.guild ?: return
        val votes = votesRepo.getAllPredictionGameVotesForWeek(guild, week)
        val countBefore = votesRepo.getVoteCountBeforeWeek(guild, week).toInt()
        val userData = userService.getData(guild, votes.mapTo(mutableSetOf()) { it.userId })
        spreadsheetService.updateSheet(config.sheetId, wait = false) {
            addAll(Coord(config.sheet, config.x, config.y + countBefore), votes.map {
                listOf(
                    week,
                    userData[it.userId]?.displayName ?: it.userId.toString(),
                    it.leagueName,
                    it.battle + 1,
                    it.idx
                )
            })
        }
    }
}
