package de.tectoast.emolga.domain.league.draft.service.timer.skipmode

import de.tectoast.emolga.domain.league.core.model.DraftState
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.NextPlayerData
import de.tectoast.emolga.domain.league.draft.model.execution.TimerSkipData
import de.tectoast.emolga.domain.league.draft.model.execution.TimerSkipResult
import de.tectoast.emolga.domain.league.draft.model.execution.defaultData
import de.tectoast.emolga.domain.league.draft.model.timer.TimerSkipMode
import de.tectoast.emolga.domain.league.draft.util.DisplayHelper
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.invoke
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class AfterDraftUnorderedTimerSkipModeHandler : TimerSkipModeHandler<TimerSkipMode.After.AfterDraftUnordered> {
    override val targetClass = TimerSkipMode.After.AfterDraftUnordered::class

    override suspend fun afterPick(
        c: TimerSkipMode.After.AfterDraftUnordered, ctx: DraftRunContext, data: NextPlayerData
    ): TimerSkipData {
        val league = ctx.league
        val draftData = league.draftData
        return if (league.draftData.moved.values.any { it.isNotEmpty() }) {
            var message: (suspend (DisplayHelper) -> K18nMessage)? = null
            if (!league.pseudoEnd) {
                message = { helper ->
                    val entriesWithMessages =
                        draftData.moved.entries.filter { it.value.isNotEmpty() }.map { (user, turns) ->
                            val ping = helper.getPingForUser(user)
                            val announceMessage = helper.buildAnnounceData(user, withTimerAnnounce = false)
                            Triple(turns.size, ping, announceMessage)
                        }
                    b {
                        K18n_League.AfterDraftUnordered(entriesWithMessages.joinToString("\n") { (turnCount, ping, message) ->
                            "${ping.content}: $turnCount${message()}"
                        })()
                    }
                }
                draftData.draftState = DraftState.PSEUDOEND
            }
            TimerSkipData(TimerSkipResult.NOCONCRETE, message = message, cancelTimer = true)

        } else TimerSkipResult.NEXT.defaultData()
    }

    override suspend fun getPickRound(c: TimerSkipMode.After.AfterDraftUnordered, ctx: DraftRunContext) =
        if (ctx.league.pseudoEnd) {
            ctx.league.movedTurns(ctx.activeIdx).removeFirst()
        } else ctx.league.round
}
