package de.tectoast.emolga.domain.league.draft.service.timer.calc

import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.domain.league.draft.model.timer.TimerInfo
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single
class SwitchTimerHandler : DraftTimerHandler<DraftTimerConfig.SwitchTimer> {
    override val targetClass = DraftTimerConfig.SwitchTimer::class

    override fun getCurrentTimerInfo(config: DraftTimerConfig.SwitchTimer, now: Instant): TimerInfo {
        return config.timerInfos[config.currentTimer]!!
    }
}