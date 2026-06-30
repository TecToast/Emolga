package de.tectoast.emolga.domain.league.draft.service.timer.skipmode

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.NextPlayerData
import de.tectoast.emolga.domain.league.draft.model.core.isNormalPick
import de.tectoast.emolga.domain.league.draft.model.execution.TimerSkipData
import de.tectoast.emolga.domain.league.draft.model.execution.TimerSkipResult
import de.tectoast.emolga.domain.league.draft.model.execution.defaultData
import de.tectoast.emolga.domain.league.draft.model.timer.TimerSkipMode
import org.koin.core.annotation.Single

@Single
class NextPickTimerSkipModeHandler : TimerSkipModeHandler<TimerSkipMode.During.NextPick> {
    override val targetClass = TimerSkipMode.During.NextPick::class

    override suspend fun afterPick(
        c: TimerSkipMode.During.NextPick, ctx: DraftRunContext, data: NextPlayerData
    ): TimerSkipData {
        return (if (data.isNormalPick()) {
            val league = ctx.league
            if (league.pseudoEnd && league.hasMovedTurns(ctx.activeIdx)) {
                league.movedTurns(ctx.activeIdx).removeFirstOrNull()
                if (league.pseudoEnd && !league.hasMovedTurns(ctx.activeIdx)) TimerSkipResult.NEXT else TimerSkipResult.SAME
            } else TimerSkipResult.NEXT
        } else TimerSkipResult.NEXT).defaultData()
    }

    override suspend fun getPickRound(c: TimerSkipMode.During.NextPick, ctx: DraftRunContext) =
        ctx.league.movedTurns(ctx.activeIdx).firstOrNull() ?: ctx.league.round
}
