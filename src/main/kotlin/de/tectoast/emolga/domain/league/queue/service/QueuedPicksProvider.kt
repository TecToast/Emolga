package de.tectoast.emolga.domain.league.queue.service

import de.tectoast.emolga.domain.league.draft.model.core.DraftInput
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.queue.model.QueuePicksUserData

interface QueuedPicksProvider {
    suspend fun getQueuedPickForUser(ctx: DraftRunContext, idx: Int, allQueuedPicks: Map<Int, QueuePicksUserData>): DraftInput?
}