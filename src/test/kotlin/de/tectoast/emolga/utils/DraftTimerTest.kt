package de.tectoast.emolga.utils

import de.tectoast.emolga.commands.defaultTimeFormat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should

class DraftTimerTest : FunSpec({
    context("Single Timer") {
        val timer = DraftTimer(TimerInfo(10, 22))
        test("InTime") {
            val now = format("24.12.2023 12:00")
            timer.testCalc(now) shouldBeTime "24.12.2023 14:00"
        }
        test("BeforeTime") {
            val now = format("24.12.2023 9:00")
            timer.testCalc(now) shouldBeTime "24.12.2023 12:00"
        }
        test("AfterTime") {
            val now = format("24.12.2023 23:00")
            timer.testCalc(now) shouldBeTime "25.12.2023 12:00"
        }

    }
    context("Multi Timer") {
        context("ShorteningTimer") {
            val timer = DraftTimer(
                0L to TimerInfo(8, 23),
                format("24.12.2023 12:00") to TimerInfo(8, 23, 10)
            )
            context("BeforeSwitch") {
                test("InTime") {
                    val now = format("22.12.2023 13:00")
                    timer.testCalc(now) shouldBeTime "22.12.2023 15:00"
                }
                test("BeforeTime") {
                    val now = format("22.12.2023 7:00")
                    timer.testCalc(now) shouldBeTime "22.12.2023 10:00"
                }
                test("AfterTime") {
                    val now = format("22.12.2023 23:00")
                    timer.testCalc(now) shouldBeTime "23.12.2023 10:00"
                }
                test("ShortlyBeforeSwitch") {
                    val now = format("24.12.2023 9:57")
                    timer.testCalc(now) shouldBeTime "24.12.2023 11:57"
                }
            }
            context("WithSwitch") {
                run {
                    val now = listOf("24.12.2023 11:00", "24.12.2023 11:57")
                    val res = "24.12.2023 12:10"
                    now.forEachIndexed { index, s ->
                        test("IntoSwitch$index") {
                            timer.testCalc(format(s)) shouldBeTime res
                        }
                    }
                }
                test("AfterSwitch1") {
                    val now = format("24.12.2023 12:00")
                    timer.testCalc(now) shouldBeTime "24.12.2023 12:10"
                }
                test("AfterSwitch2") {
                    val now = format("24.12.2023 13:00")
                    timer.testCalc(now) shouldBeTime "24.12.2023 13:10"
                }
            }
        }
        context("ExtendingTimer") {
            val timer = DraftTimer(
                0L to TimerInfo(8, 23, 10),
                format("24.12.2023 12:00") to TimerInfo(8, 23, 120)
            )
            context("BeforeSwitch") {
                test("InTime") {
                    val now = format("22.12.2023 13:00")
                    timer.testCalc(now) shouldBeTime "22.12.2023 13:10"
                }
                test("BeforeTime") {
                    val now = format("22.12.2023 7:00")
                    timer.testCalc(now) shouldBeTime "22.12.2023 8:10"
                }
                test("AfterTime") {
                    val now = format("22.12.2023 23:00")
                    timer.testCalc(now) shouldBeTime "23.12.2023 8:10"
                }
                test("ShortlyBeforeSwitch") {
                    val now = format("24.12.2023 11:47")
                    timer.testCalc(now) shouldBeTime "24.12.2023 11:57"
                }
            }
            context("WithSwitch") {
                test("IntoSwitch") {
                    timer.testCalc(format("24.12.2023 11:55")) shouldBeTime "24.12.2023 13:55"
                }
                test("AfterSwitch1") {
                    val now = format("24.12.2023 12:00")
                    timer.testCalc(now) shouldBeTime "24.12.2023 14:00"
                }
                test("AfterSwitch2") {
                    val now = format("24.12.2023 13:00")
                    timer.testCalc(now) shouldBeTime "24.12.2023 15:00"
                }
            }
        }
    }
})

private infix fun Long.shouldBeTime(str: String) = this should Matcher { value ->
    MatcherResult(value == format(str), { "${format(value)} should be $str" }, {
        "${format(value)} should not be $str"
    })
}

private fun DraftTimer.testCalc(now: Long, timerStart: Long? = null) = calc(now, timerStart) + now
private fun format(str: String) = defaultTimeFormat.parse(str).time
private fun format(date: Long) = defaultTimeFormat.format(date)