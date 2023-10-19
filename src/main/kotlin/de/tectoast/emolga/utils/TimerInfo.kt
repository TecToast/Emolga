package de.tectoast.emolga.utils

import de.tectoast.emolga.utils.records.TimerData

class TimerInfo(delaysAfterSkips: Map<Int, Int>) {
    private val map: MutableMap<Int, TimerData> = mutableMapOf()
    val delayData =
        delaysAfterSkips.also { if (it[0] == null) throw IllegalArgumentException("Delay for zero must be set!") }.entries.sortedByDescending { it.key }

    fun getDelayAfterSkips(howOftenSkipped: Int): Int {
        return delayData.first { howOftenSkipped >= it.key }.value
    }

    constructor(delayInMins: Int) : this(mapOf(0 to delayInMins))
    constructor(from: Int, to: Int, delayInMins: Int = 120) : this(delayInMins) {
        set(from, to)
    }

    fun add(from: Int, to: Int, vararg days: Int): TimerInfo {
        for (day in days) {
            map[day] = TimerData(from, to)
        }
        return this
    }

    operator fun set(from: Int, to: Int): TimerInfo {
        for (i in 1..7) {
            map[i] = TimerData(from, to)
        }
        return this
    }

    operator fun get(day: Int): TimerData {
        return map[day] ?: throw IllegalStateException("TimerInfo Map Incomplete ($day)")
    }
}
