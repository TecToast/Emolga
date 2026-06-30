package de.tectoast.emolga.domain.league.draft.service.timer

import de.tectoast.emolga.domain.league.draft.model.timer.DelayData
import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.domain.league.draft.model.timer.TimerInfo
import de.tectoast.emolga.domain.league.draft.service.timer.calc.DraftTimerDispatcher
import de.tectoast.emolga.utils.toJavaLocalDateTime
import de.tectoast.emolga.utils.toKotlinInstant
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Single
class DraftTimerCalculationService(private val clock: Clock, private val dispatcher: DraftTimerDispatcher) {
    fun calc(
        config: DraftTimerConfig,
        now: Instant = clock.now(),
        howOftenSkipped: Int = 0,
        usedStallSeconds: Int = 0
    ): DelayData? {
        lateinit var currentTimerInfo: TimerInfo
        fun currentDelay(): Int? {
            val calced = currentTimerInfo.getDelayAfterSkips(howOftenSkipped)
            return if (calced == 0 && dispatcher.shouldCancelOnZeroDelay(config)) null else calced
        }

        var localDateTime = listOfNotNull(now, config.timerStart).max().toJavaLocalDateTime()
        fun recheckTimerInfo() {
            currentTimerInfo = dispatcher.getCurrentTimerInfo(config, localDateTime.toKotlinInstant())
        }
        recheckTimerInfo()
        var currentDelay = currentDelay() ?: return null
        var minutesToGo: Int = currentDelay
        while (minutesToGo > 0) {
            val p = currentTimerInfo[localDateTime.dayOfWeek.value]
            val hour = localDateTime.hour
            if (hour >= p.from && hour < p.to) minutesToGo-- else localDateTime = localDateTime.withSecond(0)
            localDateTime = localDateTime.plusMinutes(1)
            recheckTimerInfo()
            val currentTimerInfoDelay = currentDelay() ?: return null
            if (currentDelay != currentTimerInfoDelay) {
                minutesToGo = if (currentTimerInfoDelay < currentDelay) {
                    minutesToGo.coerceAtMost(currentTimerInfoDelay)
                } else {
                    currentTimerInfoDelay - (currentDelay - minutesToGo)
                }
                currentDelay = currentTimerInfoDelay
            }
        }
        val regularTimestamp = localDateTime.toKotlinInstant()
        return DelayData(
            regularTimestamp + (config.stallSeconds - usedStallSeconds).coerceAtLeast(0).seconds,
            regularTimestamp,
            now
        )
    }


}