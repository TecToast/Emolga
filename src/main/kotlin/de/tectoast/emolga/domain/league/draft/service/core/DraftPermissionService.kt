package de.tectoast.emolga.domain.league.draft.service.core

import de.tectoast.emolga.domain.league.core.model.DraftRelevantLeagueData
import de.tectoast.emolga.domain.league.draft.model.core.PickContext
import de.tectoast.emolga.domain.league.draft.repository.DraftAdminRepository
import de.tectoast.emolga.domain.league.draft.service.util.DraftCurrentService
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class DraftPermissionService(
    private val draftAdminRepo: DraftAdminRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val draftCurrentService: DraftCurrentService
) {

    suspend fun checkPickPermission(
        league: DraftRelevantLeagueData, uid: Long, roleIds: Collection<Long>
    ): CalcResult<PickContext> = with(league) {
        val context = draftCurrentService.getCurrentUser(league, uid).getOrReturn { return it }
        if (leagueMemberRepo.isAuthorizedFor(leagueName, currentIdx, uid)) return context.success()
        if (draftAdminRepo.isAdmin(guild, uid, roleIds)) return context.success()
        return K18n_League.NotYourTurn.error()
    }
}

