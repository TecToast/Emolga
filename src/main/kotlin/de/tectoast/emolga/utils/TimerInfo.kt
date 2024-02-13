package de.tectoast.emolga.utils

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class TimerInfo {
    private val dayToData: MutableMap<Int, TimerData> = mutableMapOf()

    val delayData: @Serializable(with = TreeMapSerializer::class) TreeMap<Int, Int>

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
        for (i in 1..7) {
            dayToData[i] = TimerData(from, to)
        }
        return this
    }

    operator fun get(day: Int): TimerData {
        return dayToData[day] ?: throw IllegalStateException("TimerInfo Map Incomplete ($day)")
    }
}

@Serializable
data class TimerData(val from: Int, val to: Int)
