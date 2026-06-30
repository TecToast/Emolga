package de.tectoast.emolga.domain.league.draft.service.timer.skipmode

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.NextPlayerData
import de.tectoast.emolga.domain.league.draft.model.execution.TimerSkipData
import de.tectoast.emolga.domain.league.draft.model.timer.TimerSkipMode
import de.tectoast.emolga.utils.handler.BaseHandler

interface TimerSkipModeOperations<C : TimerSkipMode> {
    /**
     * What happens after a pick/timer skip
     * @param data the data of the pick
     * @return if the next player should be announced
     */
    suspend fun afterPick(c: C, ctx: DraftRunContext, data: NextPlayerData): TimerSkipData
    suspend fun getPickRound(c: C, ctx: DraftRunContext): Int
}

interface TimerSkipModeHandler<C : TimerSkipMode> : BaseHandler<C>, TimerSkipModeOperations<C>
