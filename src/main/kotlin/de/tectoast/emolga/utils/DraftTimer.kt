package de.tectoast.emolga.utils

import de.tectoast.emolga.features.draft.SwitchTimer
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*
import kotlin.time.measureTimedValue

@Serializable
sealed class DraftTimer {
    private val isNoMultiTimer = this !is ClockDependentTimer
    abstract fun getCurrentTimerInfo(millis: Long = System.currentTimeMillis()): TimerInfo

    var stallSeconds = 0
    fun stallSeconds(stallSeconds: Int) = apply { this.stallSeconds = stallSeconds }
    fun calc(league: League) = calc(
        timerStart = league.timerStart,
        howOftenSkipped = league.skippedTurns[league.current]?.size ?: 0,
        usedStallSeconds = league.usedStallSeconds[league.current] ?: 0
    )

    fun calc(
        now: Long = System.currentTimeMillis(),
        timerStart: Long? = null,
        howOftenSkipped: Int = 0,
        usedStallSeconds: Int = 0
    ): Long? {
        val (delay, duration) = measureTimedValue {
            lateinit var currentTimerInfo: TimerInfo
            fun currentDelay(): Int? {
                val calced = currentTimerInfo.getDelayAfterSkips(howOftenSkipped)
                return if (calced == 0 && isNoMultiTimer) null else calced
            }

            val cal = Calendar.getInstance().apply {
                timeInMillis = timerStart?.let(now::coerceAtLeast) ?: now
            }

            fun recheckTimerInfo() {
                currentTimerInfo = getCurrentTimerInfo(cal.timeInMillis)
            }
            recheckTimerInfo()
            var currentDelay = currentDelay() ?: return null
            var elapsedMinutes: Int = currentDelay
            while (elapsedMinutes > 0) {
                val p = currentTimerInfo[cal[Calendar.DAY_OF_WEEK]]
                val hour = cal[Calendar.HOUR_OF_DAY]
                if (hour >= p.from && hour < p.to) elapsedMinutes-- else cal[Calendar.SECOND] = 0
                cal.add(Calendar.MINUTE, 1)
                recheckTimerInfo()
                val currentTimerInfoDelay = currentDelay() ?: return null
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


}

@Serializable
@SerialName("CD")
class ClockDependentTimer(val timers: @Serializable(with = TreeMapSerializer::class) TreeMap<Long, TimerInfo>) :
    DraftTimer() {

    constructor(timerInfo: TimerInfo) : this(timerMap {
        put(0L, timerInfo)
    })

    constructor(vararg timers: Pair<Long, TimerInfo>) : this(timerMap {
        putAll(timers)
    })

    override fun getCurrentTimerInfo(millis: Long): TimerInfo = timers.floorEntry(millis).value

    companion object {
        fun timerMap(dsl: MutableMap<Long, TimerInfo>.() -> Unit) = TreeMap<Long, TimerInfo>().apply(dsl)
    }
}

@Serializable
@SerialName("Simple")
class SimpleTimer(val timerInfo: TimerInfo) : DraftTimer() {
    override fun getCurrentTimerInfo(millis: Long): TimerInfo = timerInfo
}

@Serializable
@SerialName("Switch")
class SwitchTimer(val timerInfos: Map<String, TimerInfo>, var currentTimer: String = timerInfos.keys.first()) :
    DraftTimer() {

    override fun getCurrentTimerInfo(millis: Long): TimerInfo {
        return timerInfos[currentTimer]!!
    }

    fun createControlPanel(league: League) = Embed {
        title = "Switch-Timer Control-Panel"
        description = "Aktueller Timer: $currentTimer"
        color = embedColor
    }.into() to timerInfos.keys.map { name ->
        SwitchTimer.Button(label = name) {
            this.league = league
            this.switchTo = name
        }
    }.into()


    fun switchTo(switchTo: String) {
        if (timerInfos.containsKey(switchTo)) {
            currentTimer = switchTo
        } else {
            throw IllegalArgumentException("Timer $switchTo does not exist!")
        }
    }
}

class TreeMapSerializer<K : Comparable<K>, V>(
    keySerializer: KSerializer<K>, valueSerializer: KSerializer<V>
) : KSerializer<TreeMap<K, V>> {

    private val mapSerializer = MapSerializer(keySerializer, valueSerializer)

    override val descriptor = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: TreeMap<K, V>) {
        mapSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): TreeMap<K, V> {
        return TreeMap(mapSerializer.deserialize(decoder))
    }
}
