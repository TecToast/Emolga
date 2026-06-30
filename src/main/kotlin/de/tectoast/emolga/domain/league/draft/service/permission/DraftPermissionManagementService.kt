package de.tectoast.emolga.domain.league.draft.service.permission

import de.tectoast.emolga.domain.league.draft.model.permission.DraftMention
import de.tectoast.emolga.domain.league.member.model.LeagueParticipant
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.util.service.LeagueQueryService
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class DraftPermissionManagementService(
    private val leagueMemberRepo: LeagueMemberRepository,
    private val leagueQueryService: LeagueQueryService
) {
    suspend fun addUser(
        guild: Long,
        user: Long,
        toAdd: Long,
        withMention: DraftMention,
        substitute: Boolean = true
    ): CalcResult<List<LeagueParticipant>> {
        val queryResult = leagueQueryService.getByGuildUser(guild, user) ?: return K18n_NoLeagueForGuildFound.error()
        addUser(queryResult.leagueName, queryResult.idx, toAdd, withMention, substitute)
        return leagueMemberRepo.getParticipantsForIdx(queryResult.leagueName, queryResult.idx).success()
    }

    private suspend fun addUser(
        leagueName: String,
        idx: Int,
        toAdd: Long,
        withMention: DraftMention,
        substitute: Boolean = true
    ) {
        leagueMemberRepo.addUser(leagueName, idx, toAdd, substitute, withMention.othermention)
        leagueMemberRepo.modifyUserPing(leagueName, idx, toAdd, withMention.selfmention)
    }

    suspend fun removeUser(guild: Long, user: Long, toRemove: Long): CalcResult<List<LeagueParticipant>> {
        val queryResult = leagueQueryService.getByGuildUser(guild, user) ?: return K18n_NoLeagueForGuildFound.error()
        leagueMemberRepo.removeUser(queryResult.leagueName, queryResult.idx, toRemove)
        return leagueMemberRepo.getParticipantsForIdx(queryResult.leagueName, queryResult.idx).success()
    }

}
