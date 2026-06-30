package de.tectoast.emolga.domain.league.draft.service.timer.calc

import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.domain.league.draft.model.timer.TimerInfo
import org.koin.core.annotation.Single
import kotlin.time.Instant


@Single
class SimpleTimerHandler : DraftTimerHandler<DraftTimerConfig.SimpleTimer> {
    override val targetClass = DraftTimerConfig.SimpleTimer::class

    override fun getCurrentTimerInfo(config: DraftTimerConfig.SimpleTimer, now: Instant): TimerInfo {
        return config.timerInfo
    }

}