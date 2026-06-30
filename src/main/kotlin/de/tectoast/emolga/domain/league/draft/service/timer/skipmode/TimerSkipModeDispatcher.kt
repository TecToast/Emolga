package de.tectoast.emolga.domain.league.draft.service.timer.skipmode

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.NextPlayerData
import de.tectoast.emolga.domain.league.draft.model.execution.TimerSkipData
import de.tectoast.emolga.domain.league.draft.model.execution.TimerSkipResult
import de.tectoast.emolga.domain.league.draft.model.execution.defaultData
import de.tectoast.emolga.domain.league.draft.model.timer.TimerSkipMode
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class TimerSkipModeDispatcher(handlers: List<TimerSkipModeHandler<TimerSkipMode>>) {
    private val registry = HandlerRegistry(handlers)

    suspend fun afterPick(
        ctx: DraftRunContext, data: NextPlayerData
    ): TimerSkipData {
        val league = ctx.league
        if (league.draftWouldEnd) {
            league.duringTimerSkipMode?.let {
                val duringResult = registry.getHandler(it).afterPick(it, ctx, data)
                if (duringResult.result == TimerSkipResult.SAME) return duringResult
            }
            return registry.getHandler(league.afterTimerSkipMode).afterPick(league.afterTimerSkipMode, ctx, data)
        }
        return league.duringTimerSkipMode?.let { registry.getHandler(it).afterPick(it, ctx, data) }
            ?: TimerSkipResult.NEXT.defaultData()
    }

    suspend fun getPickRound(
        ctx: DraftRunContext
    ): Int {
        val timerSkipMode = ctx.getCurrentTimerSkipMode()
        return registry.getHandler(timerSkipMode).getPickRound(timerSkipMode, ctx)
    }

    private fun DraftRunContext.getCurrentTimerSkipMode() =
        league.duringTimerSkipMode?.takeUnless { league.draftWouldEnd } ?: league.afterTimerSkipMode
}
