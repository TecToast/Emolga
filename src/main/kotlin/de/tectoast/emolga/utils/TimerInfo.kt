package de.tectoast.emolga.utils

import de.tectoast.emolga.utils.records.TimerData

class TimerInfo {
    private val map: MutableMap<Int, TimerData> = HashMap()
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