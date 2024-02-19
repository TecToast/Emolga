package de.tectoast.emolga.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*
import kotlin.time.Duration.Companion.seconds

object TimeUtils {
    private val DURATION_PATTERN = Regex("(\\d{1,8})([smhdwy]?)")
    private val DURATION_SPLITTER = Regex("[.|:]")
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
    private val shortToPretty = mapOf(
        "y" to ("Jahr" to "Jahre"),
        "w" to ("Woche" to "Wochen"),
        "d" to ("Tag" to "Tage"),
        "h" to ("Stunde" to "Stunden"),
        "m" to ("Minute" to "Minuten"),
        "s" to ("Sekunde " to "Sekunden")
    )
    private val STRING_TO_SECONDS = run {
        val map = TreeMap<String, Int>()
        for ((k, v) in SECONDS_TOSTRING) {
            map[v] = k
        }
        map[""] = 1
        map
    }

    fun parseShortTime(timestring: String): Long {
        var result = 0L
        timestring.split(" ").forEach {
            DURATION_PATTERN.find(it)?.destructured?.let { (amount, unit) ->
                result += amount.toInt() * STRING_TO_SECONDS[unit.lowercase()]!!
            }
        }
        return result
    }

    private fun secondsToTimeBase(timesec: Long): Map<String, Int> {
        var remaining = timesec
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

    fun secondsToTimePretty(timesec: Long): String {
        val base = secondsToTimeBase(timesec)
        val builder = StringBuilder()
        base.forEach { (k, v) ->
            val (singular, plural) = shortToPretty[k]!!
            builder.append("**").append(v).append("** ").append(pluralise(v.toLong(), singular, plural)).append(", ")
        }
        if (builder.endsWith(", ")) builder.substring(0, builder.length - 2)
        return if (builder.isEmpty()) "**0** Sekunden" else builder.toString()
    }

    fun secondsToTimeShort(timesec: Long): String {
        val base = secondsToTimeBase(timesec)
        val builder = StringBuilder()
        base.forEach { (k, v) ->
            builder.append(v).append(k).append(" ")
        }
        return builder.toString().trim()
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


@Serializable(with = IntervalSerializer::class)
class Interval(val seconds: Long) {
    constructor(str: String) : this(TimeUtils.parseShortTime(str))

    fun toDuration() = seconds.seconds
}

object IntervalSerializer : KSerializer<Interval> {
    override val descriptor = PrimitiveSerialDescriptor("Interval", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Interval) {
        encoder.encodeString(TimeUtils.secondsToTimeShort(value.seconds))
    }

    override fun deserialize(decoder: Decoder): Interval {
        return Interval(decoder.decodeString())
    }
}
