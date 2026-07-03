package de.tectoast.emolga.domain.league.result.service

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.result.repository.ResultCodesRepository
import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleRepository
import de.tectoast.emolga.features.league.K18n_EnterResult
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.K18nMessageOrError
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class ResultStartService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueScheduleRepo: LeagueScheduleRepository,
    private val resultCodesRepo: ResultCodesRepository,
    private val botConstants: BotConstants
) {
    suspend fun handleStart(opponent: Long, user: Long, guild: Long): K18nMessageOrError {
        val league = leagueCoreRepo.getLeagueWithParticipants(guild, user) ?: return K18n_NoLeagueForGuildFound.error()
        val users = league.users
        val idx1 = users.indexOf(user)
        val idx2 =
            users.indexOf(opponent).takeIf { it >= 0 }
                ?: return K18n_EnterResult.NoLeagueWithOpponent(botConstants.botOwnerTag)
                    .error()
        val scheduleData = leagueScheduleRepo.getScheduleData(league.leagueName, idx1, idx2)
            ?: return K18n_EnterResult.NoLeagueWithOpponent(botConstants.botOwnerTag).error()
        val week = scheduleData.week
        val uuid =
            resultCodesRepo.add(league.leagueName, week, idx1, idx2)
        return K18n_EnterResult.Success(botConstants.webBaseUrl, uuid.toString()).success()
    }
}