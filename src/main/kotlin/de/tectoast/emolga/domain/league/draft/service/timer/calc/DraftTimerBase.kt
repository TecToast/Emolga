package de.tectoast.emolga.domain.league.draft.service.timer.calc

import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.domain.league.draft.model.timer.TimerInfo
import de.tectoast.emolga.utils.handler.BaseHandler
import kotlin.time.Instant

interface DraftTimerOperations<C : DraftTimerConfig> {
    fun getCurrentTimerInfo(config: C, now: Instant): TimerInfo

    fun shouldCancelOnZeroDelay(config: C): Boolean
}

interface DraftTimerHandler<C : DraftTimerConfig> : DraftTimerOperations<C>, BaseHandler<C> {
    override fun shouldCancelOnZeroDelay(config: C) = true
}