package de.tectoast.emolga.utils.repeat

import io.kotest.core.spec.style.FunSpec
import mu.KotlinLogging
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}
class RepeatTaskTest : FunSpec({
    xtest("RepeatTask") {
        val (value, duration) = measureTimedValue {
            RepeatTask("Test", RepeatTaskType.Other("Test"), "13.04.2024 14:00", 7, 7.days, false) {
                logger.info(it.toString())
            }
        }
        value.taskTimestamps.forEach {
            logger.info(it.toString())
        }
        logger.info(duration.toString())
    }
})
