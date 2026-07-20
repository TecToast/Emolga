package de.tectoast.emolga.domain.util.service

import de.tectoast.emolga.utils.toJavaLocalDateTime
import de.tectoast.emolga.utils.toKotlinInstant
import org.koin.core.annotation.Single
import java.time.LocalDateTime
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Single
class TimeFormatService(private val clock: Clock) {

    fun parseDuration(timestring: String): Duration {
        var result = 0L
        timestring.split(" ").forEach {
            DURATION_PATTERN.find(it)?.destructured?.let { (amount, unit) ->
                result += amount.toInt() * STRING_TO_SECONDS[unit.lowercase()]!!
            }
        }
        return result.seconds
    }

    private fun durationToPrettyBase(duration: Duration): Map<String, Int> {
        var remaining = duration.inWholeSeconds
        val map = mutableMapOf<String, Int>()
        SECONDS_TOSTRING.entries.forEach {
            val amount = remaining / it.key
            if (amount > 0) {
                map[it.value] = amount.toInt()
                remaining %= it.key
            }
        }
        return map
    }


    fun durationToPrettyShort(duration: Duration): String {
        val base = durationToPrettyBase(duration)
        val builder = StringBuilder()
        base.forEach { (k, v) ->
            builder.append(v).append(k).append(" ")
        }
        return builder.toString().trim()
    }

    fun parseCalendarTime(str: String): Instant {
        val timestr = str.lowercase()
        val now = clock.now()
        if (timestr.any { it.isLetter() }) {
            return now + parseDuration(timestr)
        }
        var time = now.toJavaLocalDateTime().withSecond(0)
        val split = str.split(" ", ";")
        val first = split[0]
        val second = split.getOrNull(1)
        if ("." in first) {
            time = time.handleDay(first, now)
            time = time.handleTime(second, now = null)
        } else {
            time = time.handleTime(first, now)
        }
        return time.toKotlinInstant()
    }

    private fun LocalDateTime.handleDay(str: String, now: Instant): LocalDateTime {
        val date = str.split(".")
        var newDate = this.withDayOfMonth(date[0].toInt())
        date.getOrNull(1)?.toIntOrNull()?.let { newDate = newDate.withMonth(it) }
        date.getOrNull(2)?.toIntOrNull()?.let { newDate = newDate.withYear(it) }
        if (newDate.toKotlinInstant() < now && date.getOrNull(2) == null) {
            newDate = newDate.plusYears(1)
        }
        return newDate
    }

    private fun LocalDateTime.handleTime(strInput: String?, now: Instant?): LocalDateTime {
        val str = strInput ?: "15:00"
        val time = str.split(":")
        var newTime = this.withHour(time[0].toInt()).withMinute(time.getOrNull(1)?.toInt() ?: 0)
        if (now != null && newTime.toKotlinInstant() < now) {
            newTime = newTime.plusDays(1)
        }
        return newTime
    }

    companion object {
        private val SECONDS_TOSTRING = TreeMap<Int, String>(
            Comparator.reverseOrder()
        ).apply {
            putAll(
                listOf(
                    60 * 60 * 24 * 365 to "y",
                    60 * 60 * 24 * 7 to "w",
                    60 * 60 * 24 to "d",
                    60 * 60 to "h",
                    60 to "m",
                    1 to "s"
                )
            )
        }
        private val STRING_TO_SECONDS = run {
            val map = TreeMap<String, Int>()
            for ((k, v) in SECONDS_TOSTRING) {
                map[v] = k
            }
            map[""] = 1
            map
        }

        private val DURATION_PATTERN = Regex("(\\d{1,8})([${STRING_TO_SECONDS.keys.joinToString("")}])")
    }
}

