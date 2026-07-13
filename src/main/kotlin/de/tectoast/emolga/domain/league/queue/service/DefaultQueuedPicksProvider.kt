package de.tectoast.emolga.domain.league.queue.service

import de.tectoast.emolga.domain.league.draft.model.core.DraftInput
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.queue.model.QueuePicksUserData
import org.koin.core.annotation.Single

@Single
class DefaultQueuedPicksProvider : QueuedPicksProvider {
    override suspend fun getQueuedPickForUser(
        ctx: DraftRunContext,
        idx: Int,
        allQueuedPicks: Map<Int, QueuePicksUserData>
    ): DraftInput? {
        ctx.config.draftBan?.banRounds[ctx.league.round]?.let { return null }
        return allQueuedPicks[idx]?.takeIf { it.enabled }?.queued?.firstOrNull()?.buildDraftInput()
    }
}