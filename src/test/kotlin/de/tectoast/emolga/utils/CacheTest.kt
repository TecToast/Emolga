package de.tectoast.emolga.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalTime::class)
class CacheTest : FunSpec({
    var x = 0
    beforeTest {
        x = 0
    }
    context("NormalCaches") {
        test("OneTimeCache") {
            val cache = OneTimeCache {
                ++x
            }
            x shouldBe 0
            cache() shouldBe 1
            x shouldBe 1
            cache() shouldBe 1
        }
        test("TimedCache") {
            var currentTime = 0L
            val testClock = object : Clock {
                override fun now(): kotlin.time.Instant {
                    return kotlin.time.Instant.fromEpochMilliseconds(currentTime)
                }
            }
            val cache = TimedCache(1000.milliseconds, testClock) {
                ++x
            }
            x shouldBe 0
            cache() shouldBe 1
            x shouldBe 1
            cache() shouldBe 1
            currentTime += 1200
            cache() shouldBe 2
            x shouldBe 2
        }
    }
    context("MappedCache") {
        beforeTest {
            cacheTest = 0
        }
        test("WithOneTimeCache") {
            val oneTime = TimedCache(Duration.INFINITE) { ++x }
            val cache = MappedCache(oneTime) { it + 10 }
            x shouldBe 0
            cache() shouldBe 11
            x shouldBe 1
            oneTime() shouldBe 1
        }
        test("WithTimedCache") {
            var currentTime = 0L
            val testClock = object : Clock {
                override fun now(): kotlin.time.Instant {
                    return kotlin.time.Instant.fromEpochMilliseconds(currentTime)
                }
            }
            val timed = TimedCache(1000.milliseconds, testClock) { ++x }
            val cache = MappedCache(timed) { it + 10 }
            x shouldBe 0
            cache() shouldBe 11
            x shouldBe 1
            timed() shouldBe 1
            currentTime += 1200
            cache() shouldBe 12
            x shouldBe 2
            timed() shouldBe 2
            currentTime += 500
            cache() shouldBe 12
            x shouldBe 2
            timed() shouldBe 2
            currentTime += 1500
            cache() shouldBe 13
            x shouldBe 3
            timed() shouldBe 3
        }
        test("WithTimedCache2") {
            var currentTime = 0L
            val testClock = object : Clock {
                override fun now(): kotlin.time.Instant {
                    return kotlin.time.Instant.fromEpochMilliseconds(currentTime)
                }
            }
            val timed = TimedCache(5.seconds, testClock) { cacheFun() }
            val cache = MappedCache(timed) { "$it :D" }

            cache() shouldBe "1 :D"
            currentTime += 1100
            cache() shouldBe "1 :D"
            currentTime += 1100
            cache() shouldBe "1 :D"
            currentTime += 4100
            cache() shouldBe "2 :D"
            currentTime += 1100
            cache() shouldBe "2 :D"
            currentTime += 4100
            cache() shouldBe "3 :D"
        }
        test("WithTimedCache3") {
            var currentTime = 0L
            val testClock = object : Clock {
                override fun now(): kotlin.time.Instant {
                    return kotlin.time.Instant.fromEpochMilliseconds(currentTime)
                }
            }
            val timed = TimedCache(2.seconds, testClock) { cacheFun() }
            var mapCounter = 0
            val cache = MappedCache(timed) { "$it ${++mapCounter} :D" }
            cache() shouldBe "1 1 :D"
            currentTime += 3000
            timed()
            currentTime += 500
            cache() shouldBe "2 2 :D"
        }
    }
})

private var cacheTest = 0
private fun cacheFun(): Int {
    logger.info("CacheFun")
    return ++cacheTest
}
