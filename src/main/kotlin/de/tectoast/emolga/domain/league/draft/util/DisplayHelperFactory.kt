package de.tectoast.emolga.domain.league.draft.util

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.draft.service.util.DraftAnnounceService
import de.tectoast.emolga.domain.league.member.service.LeagueMentionService
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class DisplayHelperFactory(
    private val announceService: DraftAnnounceService,
    private val picksRepo: LeaguePickRepository,
    private val leagueMentionService: LeagueMentionService
) {
    fun create(ctx: DraftRunContext): DisplayHelper =
        MainDisplayHelper(announceService, picksRepo, leagueMentionService, ctx)
}


private class MainDisplayHelper(
    val announceService: DraftAnnounceService,
    val picksRepo: LeaguePickRepository,
    val leagueMentionService: LeagueMentionService,
    val ctx: DraftRunContext,
) : DisplayHelper {
    override suspend fun buildAnnounceData(idx: Int, withTimerAnnounce: Boolean): K18nMessage {
        return announceService.generateAnnounceData(
            picksRepo.getPicksForUser(ctx.league.leagueName, idx), withTimerAnnounce, ctx, ctx.tierlistMeta
        )
    }

    override suspend fun getPingForUser(idx: Int) =
        leagueMentionService.getMentionForParticipant(ctx.league.leagueName, idx)
}
