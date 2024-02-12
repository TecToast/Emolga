package de.tectoast.emolga.utils

import de.tectoast.emolga.utils.json.emolga.draft.League
import java.util.*
import kotlin.time.measureTimedValue

class DraftTimer(
    timersMap: SortedMap<Long, TimerInfo>
) {
    val timers = timersMap.entries
    var stallSeconds = 0

    constructor(timerInfo: TimerInfo) : this(timerMap {
        put(0, timerInfo)
    })

    constructor(vararg timers: Pair<Long, TimerInfo>) : this(timerMap {
        putAll(timers)
    })

    fun stallSeconds(stallSeconds: Int) = apply { this.stallSeconds = stallSeconds }


    fun getCurrentTimerInfo(millis: Long = System.currentTimeMillis()) = timers.first { it.key <= millis }.value
    fun calc(league: League) =
        calc(
            timerStart = league.timerStart,
            howOftenSkipped = league.skippedTurns[league.current]?.size ?: 0,
            usedStallSeconds = league.usedStallSeconds[league.current] ?: 0
        )

    fun calc(
        now: Long = System.currentTimeMillis(),
        timerStart: Long? = null,
        howOftenSkipped: Int = 0,
        usedStallSeconds: Int = 0
    ): Long {
        val (delay, duration) = measureTimedValue {
            lateinit var currentTimerInfo: TimerInfo
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
            }

            fun recheckTimerInfo() {
                currentTimerInfo = timers.first { it.key <= cal.timeInMillis }.value
            }

            fun currentDelay() = currentTimerInfo.getDelayAfterSkips(howOftenSkipped)
            val ctm = cal.timeInMillis
            cal.timeInMillis = timerStart?.let { if (it > ctm) it else ctm } ?: ctm
            recheckTimerInfo()
            var currentDelay = currentDelay()
            var elapsedMinutes: Int = currentDelay
            var firstIteration = true
            while (elapsedMinutes > 0) {
                val p = currentTimerInfo[cal[Calendar.DAY_OF_WEEK]]
                val hour = cal[Calendar.HOUR_OF_DAY]
                if (hour >= p.from && hour < p.to) elapsedMinutes-- else if (firstIteration) cal[Calendar.SECOND] = 0
                firstIteration = false
                cal.add(Calendar.MINUTE, 1)
                recheckTimerInfo()
                val currentTimerInfoDelay = currentDelay()
                if (currentDelay != currentTimerInfoDelay) {
                    elapsedMinutes = if (currentTimerInfoDelay < currentDelay) {
                        elapsedMinutes.coerceAtMost(currentTimerInfoDelay)
                    } else {
                        currentTimerInfoDelay - (currentDelay - elapsedMinutes)
                    }
                    currentDelay = currentTimerInfoDelay
                }
            }
            cal.add(Calendar.SECOND, (stallSeconds - usedStallSeconds).coerceAtLeast(0))
            cal.timeInMillis - now
//        cal.add(Calendar.MINUTE, elapsedMinutes)
        }
        println("DURATION FOR TIMER CALC: ${duration.inWholeMilliseconds}ms")
        return delay
    }

    companion object {
        fun timerMap(dsl: MutableMap<Long, TimerInfo>.() -> Unit) =
            TreeMap<Long, TimerInfo>(Comparator.reverseOrder()).apply(dsl)
    }
}
