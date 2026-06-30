package de.tectoast.emolga.domain.league.draft.model.timer

import de.tectoast.emolga.utils.serializer.TreeMapSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class TimerInfo {
    private val dayToData: MutableMap<Int, TimerData> = mutableMapOf()
    private var globalTimerData: TimerData? = null

    private val delayData: @Serializable(with = TreeMapSerializer::class) TreeMap<Int, Int>

    fun getDelayAfterSkips(howOftenSkipped: Int): Int = delayData.floorEntry(howOftenSkipped).value

    constructor(delaysAfterSkips: Map<Int, Int>) {
        if (delaysAfterSkips.isEmpty()) throw IllegalArgumentException("delaysAfterSkips must not be empty")
        delayData = TreeMap(delaysAfterSkips)
    }

    constructor(delayInMins: Int) : this(mapOf(0 to delayInMins))

    operator fun set(from: Int, to: Int): TimerInfo {
        globalTimerData = TimerData(from, to)
        return this
    }

    operator fun get(day: Int): TimerData {
        return dayToData[day] ?: globalTimerData ?: TimerData(0, 24)
    }
}

