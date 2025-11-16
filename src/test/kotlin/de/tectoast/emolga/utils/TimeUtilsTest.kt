package de.tectoast.emolga.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeBetween
import io.kotest.matchers.shouldBe
import java.util.*

private val Long.f get() = defaultTimeFormat.format(this)
class TimeUtilsTest : FunSpec({

    test("parseShortTimeNew") {
        TimeUtils.parseShortTime("1d") shouldBe 86400
        TimeUtils.parseShortTime("2d") shouldBe 86400 * 2
        TimeUtils.parseShortTime("90m") shouldBe 90 * 60
        TimeUtils.parseShortTime("1m 30s") shouldBe 90
    }
    test("calendar") {
        println("GuMo")
        println(Calendar.getInstance().timeInMillis.f)
        println(TimeUtils.parseCalendarTime("19").f)
        println(TimeUtils.parseCalendarTime("15").f)
        println(TimeUtils.parseCalendarTime("15:00").f)
        println(TimeUtils.parseCalendarTime("10.5. 12:42").f)
        println(TimeUtils.parseCalendarTime("2h 30").f)
        val calced = TimeUtils.parseCalendarTime("01.01.2025 12:00")
        calced.shouldBeBetween(1735729200000, 1735729200000 + 1000)
    }
})
