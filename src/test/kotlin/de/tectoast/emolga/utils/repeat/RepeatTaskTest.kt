package de.tectoast.emolga.utils.repeat

import io.kotest.core.spec.style.FunSpec
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTimedValue

class RepeatTaskTest : FunSpec({
    test("RepeatTask") {
        val (value, duration) = measureTimedValue {
            RepeatTask("13.04.2024 14:00", 7, 7.days, false) {
                println(it)
            }
        }
        value.allTimestamps.forEach {
            println(it)
        }
        println(duration)
    }
})
