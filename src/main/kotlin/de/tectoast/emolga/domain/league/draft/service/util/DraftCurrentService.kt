package de.tectoast.emolga.domain.league.draft.service.util

import de.tectoast.emolga.domain.league.core.model.DraftRelevantLeagueData
import de.tectoast.emolga.domain.league.draft.model.core.PickContext
import de.tectoast.emolga.domain.league.draft.model.timer.TimerSkipMode
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class DraftCurrentService(private val leagueMemberRepo: LeagueMemberRepository) {
    suspend fun getCurrentUser(league: DraftRelevantLeagueData, uid: Long): CalcResult<PickContext> = with(league) {
        val idxOfParticipant = leagueMemberRepo.getIdxOfParticipant(leagueName, uid)
        if (pseudoEnd && afterTimerSkipMode == TimerSkipMode.After.AfterDraftUnordered) {
            if (idxOfParticipant == null) {
                leagueMemberRepo.getSingleParticipantAsSubstitute(leagueName, uid)?.takeIf { hasMovedTurns(it) }?.let {
                    return PickContext.AfterDraftUnordered(it).success()
                }
                return K18n_League.NoOpenPicks.error()
            }
            if (hasMovedTurns(idxOfParticipant)) return PickContext.AfterDraftUnordered(idxOfParticipant).success()
            return K18n_League.NoOpenPicks.error()
        }
        if (potentialBetweenPick && idxOfParticipant != null) {
            if (hasMovedTurns(idxOfParticipant)) return PickContext.InBetweenPick(
                idxOfParticipant,
                isActualCurrent = currentIdx == idxOfParticipant
            ).success()
        }
        PickContext.RegularTurn(currentIdx).success()
    }
}
