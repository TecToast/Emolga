package de.tectoast.emolga.utils.repeat

import io.kotest.core.spec.style.FunSpec
import mu.KotlinLogging
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}
class RepeatTaskTest : FunSpec({
    test("RepeatTask") {
        val (value, duration) = measureTimedValue {
            RepeatTask("13.04.2024 14:00", 7, 7.days, false) {
                logger.info(it.toString())
            }
        }
        value.allTimestamps.forEach {
            logger.info(it.toString())
        }
        logger.info(duration.toString())
    }
})
