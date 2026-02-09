package de.tectoast.emolga.utils

import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object TimeUtils {
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
    private val shortToPretty = mapOf(
        "y" to (K18n_TimeUtils.YearSingular to K18n_TimeUtils.YearPlural),
        "w" to (K18n_TimeUtils.WeekSingular to K18n_TimeUtils.WeekPlural),
        "d" to (K18n_TimeUtils.DaySingular to K18n_TimeUtils.DayPlural),
        "h" to (K18n_TimeUtils.HourSingular to K18n_TimeUtils.HourPlural),
        "m" to (K18n_TimeUtils.MinuteSingular to K18n_TimeUtils.MinutePlural),
        "s" to (K18n_TimeUtils.SecondSingular to K18n_TimeUtils.SecondPlural)
    )
    private val DURATION_PATTERN = Regex("(\\d{1,8})([${STRING_TO_SECONDS.keys.joinToString("")}])")
    private val DURATION_SPLITTER = Regex("[.|:]")

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

    fun secondsToTimePretty(timesec: Long, language: K18nLanguage) =
        secondsToTimeBase(timesec).entries.joinToString { (k, v) ->
        "**$v** ${
            pluralise(
                v.toLong(), shortToPretty[k]!!
            ).translateTo(language)
        }"
        }.ifEmpty { "**0** ${K18n_TimeUtils.SecondPlural.translateTo(language)}" }

    fun secondsToTimeShort(timesec: Long): String {
        val base = secondsToTimeBase(timesec)
        val builder = StringBuilder()
        base.forEach { (k, v) ->
            builder.append(v).append(k).append(" ")
        }
        return builder.toString().trim()
    }

    fun parseCalendarTime(str: String): Long {
        val timestr = str.lowercase()
        if (timestr.any { it.isLetter() }) {
            return System.currentTimeMillis() + parseShortTime(timestr) * 1000
        }
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar[Calendar.SECOND] = 0
        val split = str.split(" ", ";")
        val first = split[0]
        val second = split.getOrNull(1)
        if ("." in first) {
            calendar.handleDay(first, now)
            calendar.handleTime(second, now = null)
        } else {
            calendar.handleTime(first, now)
        }
        return calendar.timeInMillis
    }

    private fun Calendar.handleDay(str: String, now: Long) {
        val date = str.split(".")
        this[Calendar.DAY_OF_MONTH] = date[0].toInt()
        date.getOrNull(1)?.toIntOrNull()?.let { this[Calendar.MONTH] = it - 1 }
        date.getOrNull(2)?.toIntOrNull()?.let { this[Calendar.YEAR] = it }
        if (this.timeInMillis < now && date.getOrNull(2) == null) {
            this.add(Calendar.YEAR, 1)
        }
    }

    private fun Calendar.handleTime(strInput: String?, now: Long?) {
        val str = strInput ?: "15:00"
        val time = str.split(":")
        this[Calendar.HOUR_OF_DAY] = time[0].toInt()
        this[Calendar.MINUTE] = time.getOrNull(1)?.toInt() ?: 0
        if (now != null && this.timeInMillis < now) {
            this.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun pluralise(x: Long, singular: K18nMessage, plural: K18nMessage) = if (x == 1L) singular else plural
    private fun pluralise(x: Long, pair: Pair<K18nMessage, K18nMessage>) = pluralise(x, pair.first, pair.second)
}


object DurationSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor("Interval", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString(TimeUtils.secondsToTimeShort(value.inWholeSeconds))
    }

    override fun deserialize(decoder: Decoder): Duration {
        return TimeUtils.parseShortTime(decoder.decodeString()).seconds
    }
}
