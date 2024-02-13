package de.tectoast.emolga.utils

import java.util.*

object TimeUtils {
    private val DURATION_PATTERN = Regex("\\d{1,8}[smhd]?")
    private val DURATION_SPLITTER = Regex("[.|:]")

    /**
     * Parses a time string into seconds.
     */
    fun parseShortTime(timestring: String): Int {
        var timestr = timestring
        timestr = timestr.lowercase()
        if (!DURATION_PATTERN.matches(timestr)) return -1
        var multiplier = 1
        when (timestr[timestr.length - 1]) {
            'd' -> {
                multiplier *= 24
                multiplier *= 60
                multiplier *= 60
                timestr = timestr.substring(0, timestr.length - 1)
            }

            'h' -> {
                multiplier *= 60
                multiplier *= 60
                timestr = timestr.substring(0, timestr.length - 1)
            }

            'm' -> {
                multiplier *= 60
                timestr = timestr.substring(0, timestr.length - 1)
            }

            's' -> timestr = timestr.substring(0, timestr.length - 1)
            else -> {}
        }
        return multiplier * timestr.toInt()
    }


    fun secondsToTime(timesec: Long): String {
        var timeseconds = timesec
        val builder = StringBuilder(20)
        val years = (timeseconds / (60 * 60 * 24 * 365)).toInt()
        if (years > 0) {
            builder.append("**").append(years).append("** ").append(pluralise(years.toLong(), "Jahr", "Jahre"))
                .append(", ")
            timeseconds %= (60 * 60 * 24 * 365)
        }
        val weeks = (timeseconds / (60 * 60 * 24 * 7)).toInt()
        if (weeks > 0) {
            builder.append("**").append(weeks).append("** ").append(pluralise(weeks.toLong(), "Woche", "Wochen"))
                .append(", ")
            timeseconds %= (60 * 60 * 24 * 7)
        }
        val days = (timeseconds / (60 * 60 * 24)).toInt()
        if (days > 0) {
            builder.append("**").append(days).append("** ").append(pluralise(days.toLong(), "Tag", "Tage")).append(", ")
            timeseconds %= (60 * 60 * 24)
        }
        val hours = (timeseconds / (60 * 60)).toInt()
        if (hours > 0) {
            builder.append("**").append(hours).append("** ").append(pluralise(hours.toLong(), "Stunde", "Stunden"))
                .append(", ")
            timeseconds %= (60 * 60)
        }
        val minutes = (timeseconds / 60).toInt()
        if (minutes > 0) {
            builder.append("**").append(minutes).append("** ").append(pluralise(minutes.toLong(), "Minute", "Minuten"))
                .append(", ")
            timeseconds %= 60
        }
        if (timeseconds > 0) {
            builder.append("**").append(timeseconds).append("** ").append(pluralise(timeseconds, "Sekunde", "Sekunden"))
        }
        var str = builder.toString()
        if (str.endsWith(", ")) str = str.substring(0, str.length - 2)
        if (str.isEmpty()) str = "**0** Sekunden"
        return str
    }

    fun parseCalendarTime(str: String): Long {
        var timestr = str.lowercase()
        if (!DURATION_PATTERN.matches(timestr)) {
            val calendar = Calendar.getInstance()
            calendar[Calendar.SECOND] = 0
            var hoursSet = false
            for (s in str.split(";", " ")) {
                val split = DURATION_SPLITTER.split(s)
                if (s.contains(".")) {
                    calendar[Calendar.DAY_OF_MONTH] = split[0].toInt()
                    calendar[Calendar.MONTH] = split[1].toInt() - 1
                } else if (s.contains(":")) {
                    calendar[Calendar.HOUR_OF_DAY] = split[0].toInt()
                    calendar[Calendar.MINUTE] = split[1].toInt()
                    hoursSet = true
                }
            }
            if (!hoursSet) {
                calendar[Calendar.HOUR_OF_DAY] = 15
                calendar[Calendar.MINUTE] = 0
            }
            return calendar.timeInMillis
        }
        var multiplier = 1000
        when (timestr[timestr.length - 1]) {
            'w' -> {
                multiplier *= 7
                multiplier *= 24
                multiplier *= 60
                multiplier *= 60
                timestr = timestr.substring(0, timestr.length - 1)
            }

            'd' -> {
                multiplier *= 24
                multiplier *= 60
                multiplier *= 60
                timestr = timestr.substring(0, timestr.length - 1)
            }

            'h' -> {
                multiplier *= 60
                multiplier *= 60
                timestr = timestr.substring(0, timestr.length - 1)
            }

            'm' -> {
                multiplier *= 60
                timestr = timestr.substring(0, timestr.length - 1)
            }

            's' -> timestr = timestr.substring(0, timestr.length - 1)
        }
        return System.currentTimeMillis() + multiplier.toLong() * timestr.toInt()
    }

    private fun pluralise(x: Long, singular: String, plural: String): String {
        return if (x == 1L) singular else plural
    }
}
