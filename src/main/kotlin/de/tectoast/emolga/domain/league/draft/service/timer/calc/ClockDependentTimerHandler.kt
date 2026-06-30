package de.tectoast.emolga.domain.league.draft.service.timer.calc

import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.domain.league.draft.model.timer.TimerInfo
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single
class ClockDependentTimerHandler : DraftTimerHandler<DraftTimerConfig.ClockDependentTimer> {
    override val targetClass = DraftTimerConfig.ClockDependentTimer::class

    override fun getCurrentTimerInfo(config: DraftTimerConfig.ClockDependentTimer, now: Instant): TimerInfo {
        return config.timers.floorEntry(now.toEpochMilliseconds()).value
    }

    override fun shouldCancelOnZeroDelay(config: DraftTimerConfig.ClockDependentTimer) = false
}