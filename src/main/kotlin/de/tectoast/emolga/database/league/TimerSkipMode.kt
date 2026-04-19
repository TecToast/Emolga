package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.BaseHandler
import de.tectoast.emolga.database.exposed.HandlerRegistry
import de.tectoast.emolga.league.DraftState
import de.tectoast.emolga.league.K18n_League
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.serialization.Serializable


sealed interface TimerSkipMode


@Serializable
sealed interface DuringTimerSkipMode : TimerSkipMode {
    @Serializable
    data object NextPick : DuringTimerSkipMode

    @Serializable
    data object Always : DuringTimerSkipMode
}

@Serializable
sealed interface AfterTimerSkipMode : TimerSkipMode {
    @Serializable
    data object AfterDraftUnordered : AfterTimerSkipMode
}

interface TimerSkipModeOperations<C : TimerSkipMode> {
    /**
     * What happens after a pick/timer skip
     * @param data the data of the pick
     * @return if the next player should be announced
     */
    suspend fun afterPick(c: C, ctx: DraftRunContext, data: NextPlayerData): TimerSkipData
    suspend fun getPickRound(c: C, ctx: DraftRunContext): Int

    fun disableTimer(c: C, ctx: DraftRunContext): Boolean
}

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

    fun disableTimer(
        c: TimerSkipMode, ctx: DraftRunContext
    ): Boolean {
        val timerSkipMode = ctx.getCurrentTimerSkipMode()
        return registry.getHandler(timerSkipMode).disableTimer(timerSkipMode, ctx)
    }

    private fun DraftRunContext.getCurrentTimerSkipMode() =
        league.duringTimerSkipMode?.takeUnless { league.draftWouldEnd } ?: league.afterTimerSkipMode
}

interface TimerSkipModeHandler<C : TimerSkipMode> : BaseHandler<C>, TimerSkipModeOperations<C> {
    override fun disableTimer(c: C, ctx: DraftRunContext) = false
}

class NextPickTimerSkipModeHandler : TimerSkipModeHandler<DuringTimerSkipMode.NextPick> {
    override val targetClass = DuringTimerSkipMode.NextPick::class

    override suspend fun afterPick(
        c: DuringTimerSkipMode.NextPick, ctx: DraftRunContext, data: NextPlayerData
    ): TimerSkipData {
        return (if (data.isNormalPick()) {
            val league = ctx.league
            if (league.pseudoEnd && league.hasMovedTurns()) {
                league.movedTurns().removeFirstOrNull()
                if (league.pseudoEnd && !league.hasMovedTurns()) TimerSkipResult.NEXT else TimerSkipResult.SAME
            } else TimerSkipResult.NEXT
        } else TimerSkipResult.NEXT).defaultData()
    }

    override suspend fun getPickRound(c: DuringTimerSkipMode.NextPick, ctx: DraftRunContext) =
        ctx.league.movedTurns().firstOrNull() ?: ctx.league.round
}

class AlwaysTimerSkipModeHandler : TimerSkipModeHandler<DuringTimerSkipMode.Always> {
    override val targetClass = DuringTimerSkipMode.Always::class

    override suspend fun afterPick(
        c: DuringTimerSkipMode.Always, ctx: DraftRunContext, data: NextPlayerData
    ): TimerSkipData {
        if (data is NextPlayerData.InBetween) {
            ctx.league.movedTurns().removeFirstOrNull()
            return TimerSkipResult.SAME.defaultData()
        }
        return TimerSkipResult.NEXT.defaultData()
    }

    override suspend fun getPickRound(c: DuringTimerSkipMode.Always, ctx: DraftRunContext) =
        ctx.league.movedTurns().firstOrNull() ?: ctx.league.round
}

class AfterDraftUnorderedTimerSkipModeHandler : TimerSkipModeHandler<AfterTimerSkipMode.AfterDraftUnordered> {
    override val targetClass = AfterTimerSkipMode.AfterDraftUnordered::class

    override suspend fun afterPick(
        c: AfterTimerSkipMode.AfterDraftUnordered, ctx: DraftRunContext, data: NextPlayerData
    ): TimerSkipData {
        val league = ctx.league
        val draftData = league.draftData
        return if (league.draftData.moved.values.any { it.isNotEmpty() }) {
            var message: (suspend (DisplayHelper) -> K18nMessage)? = null
            if (!league.pseudoEnd) {
                message = { helper ->
                    K18n_League.AfterDraftUnordered(draftData.moved.entries.filter { it.value.isNotEmpty() }
                        .map { (user, turns) ->
                            "${helper.getPingForUser(user)}: ${turns.size}${
                                helper.buildAnnounceData(user)
                            }"
                        }.joinToString("\n"))
                }
                draftData.draftState = DraftState.PSEUDOEND
            }
            TimerSkipData(TimerSkipResult.NOCONCRETE, message = message, cancelTimer = true)

        } else TimerSkipResult.NEXT.defaultData()
    }

    override suspend fun getPickRound(c: AfterTimerSkipMode.AfterDraftUnordered, ctx: DraftRunContext) =
        if (ctx.league.pseudoEnd) {
            ctx.league.movedTurns().removeFirst()
        } else ctx.league.round

    override fun disableTimer(c: AfterTimerSkipMode.AfterDraftUnordered, ctx: DraftRunContext): Boolean {
        return ctx.league.pseudoEnd
    }
}
