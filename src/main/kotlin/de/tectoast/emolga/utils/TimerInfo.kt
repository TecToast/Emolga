package de.tectoast.emolga.utils

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class TimerInfo {
    private val dayToData: MutableMap<Int, TimerData> = mutableMapOf()
    private var globalTimerData: TimerData? = null

    val delayData: @Serializable(with = TreeMapSerializer::class) TreeMap<Int, Int>
    var startPunishSkipsTime: Long = 0

    fun getDelayAfterSkips(howOftenSkipped: Int): Int = delayData.floorEntry(howOftenSkipped).value

    constructor(delaysAfterSkips: Map<Int, Int>) {
        if (delaysAfterSkips.isEmpty()) throw IllegalArgumentException("delaysAfterSkips must not be empty")
        delayData = TreeMap(delaysAfterSkips)
    }
    constructor(delayInMins: Int) : this(mapOf(0 to delayInMins))
    constructor(from: Int, to: Int, delayInMins: Int = 120) : this(delayInMins) {
        set(from, to)
    }

    fun add(from: Int, to: Int, vararg days: Int): TimerInfo {
        for (day in days) {
            dayToData[day] = TimerData(from, to)
        }
        return this
    }

    operator fun set(from: Int, to: Int): TimerInfo {
        globalTimerData = TimerData(from, to)
        return this
    }

    operator fun get(day: Int): TimerData {
        return dayToData[day] ?: globalTimerData ?: TimerData(0, 24)
    }
}

@Serializable
data class TimerData(val from: Int, val to: Int)
