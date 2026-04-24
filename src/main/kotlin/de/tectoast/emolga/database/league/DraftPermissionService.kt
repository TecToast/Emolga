package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.DraftAdminRepository
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.json.CalcResult
import de.tectoast.emolga.utils.json.error
import de.tectoast.emolga.utils.json.success

class DraftPermissionService(val draftAdminRepo: DraftAdminRepository, val leagueMemberRepo: LeagueMemberRepository) {

    suspend fun checkPickPermission(
        league: DraftRelevantLeagueData, uid: Long, roleIds: Collection<Long>
    ): CalcResult<PickContext> = with(league) {
        val idxOfParticipant = leagueMemberRepo.getIdxOfParticipant(leagueName, uid)
        if (pseudoEnd && afterTimerSkipMode == AfterTimerSkipMode.AfterDraftUnordered) {
            if (idxOfParticipant == null) {
                leagueMemberRepo.getSingleParticipantAsSubstitute(leagueName, uid)?.takeIf { hasMovedTurns(it) }?.let {
                    return PickContext.AfterDraftUnordered(it).success<PickContext>()
                }
                return K18n_League.NoOpenPicks.error<PickContext>()
            }
            if (hasMovedTurns(idxOfParticipant)) return PickContext.AfterDraftUnordered(idxOfParticipant).success<PickContext>()
            return K18n_League.NoOpenPicks.error<PickContext>()
        }
        if (potentialBetweenPick && idxOfParticipant != null) {
            if (hasMovedTurns(idxOfParticipant)) return PickContext.InBetweenPick(idxOfParticipant, isActualCurrent = currentIdx == idxOfParticipant).success<PickContext>()
        }
        if (leagueMemberRepo.isAuthorizedFor(leagueName, currentIdx, uid)) return PickContext.RegularTurn.success<PickContext>()
        if (draftAdminRepo.isAdmin(guild, uid, roleIds)) return PickContext.RegularTurn.success<PickContext>()
        return K18n_League.NotYourTurn.error<PickContext>()
    }
}