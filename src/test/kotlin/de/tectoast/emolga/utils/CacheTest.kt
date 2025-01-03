package de.tectoast.emolga.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import mu.KotlinLogging
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}
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
            val cache = TimedCache(1000.milliseconds) {
                ++x
            }
            x shouldBe 0
            cache() shouldBe 1
            x shouldBe 1
            cache() shouldBe 1
            delay(1000)
            cache() shouldBe 2
            x shouldBe 2
        }
    }
    context("MappedCache") {
        beforeTest {
            cacheTest = 0
        }
        test("WithOneTimeCache") {
            val oneTime = OneTimeCache { ++x }
            val cache = MappedCache(oneTime) { it + 10 }
            x shouldBe 0
            cache() shouldBe 11
            x shouldBe 1
            oneTime() shouldBe 1
        }
        test("WithTimedCache") {
            val timed = TimedCache(1000.milliseconds) { ++x }
            val cache = MappedCache(timed) { it + 10 }
            x shouldBe 0
            cache() shouldBe 11
            x shouldBe 1
            timed() shouldBe 1
            delay(1000)
            cache() shouldBe 12
            x shouldBe 2
            timed() shouldBe 2
            delay(500)
            cache() shouldBe 12
            x shouldBe 2
            timed() shouldBe 2
            delay(1500)
            cache() shouldBe 13
            x shouldBe 3
            timed() shouldBe 3
        }
        test("WithTimedCache2") {
            val timed = TimedCache(5.seconds) { cacheFun() }
            val cache = MappedCache(timed) { "$it :D" }

            cache() shouldBe "1 :D"
            delay(1000)
            cache() shouldBe "1 :D"
            delay(1000)
            cache() shouldBe "1 :D"
            delay(4000)
            cache() shouldBe "2 :D"
            delay(1000)
            cache() shouldBe "2 :D"
            delay(4000)
            cache() shouldBe "3 :D"
        }
        test("WithTimedCache3") {
            val timed = TimedCache(2.seconds) { cacheFun() }
            var mapCounter = 0
            val cache = MappedCache(timed) { "$it ${++mapCounter} :D" }
            cache() shouldBe "1 1 :D"
            delay(3000)
            timed()
            delay(500)
            cache() shouldBe "2 2 :D"
        }
    }
})

private var cacheTest = 0
private fun cacheFun(): Int {
    logger.info("CacheFun")
    return ++cacheTest
}
