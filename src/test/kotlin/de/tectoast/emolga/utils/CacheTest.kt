package de.tectoast.emolga.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
        }
    }
})
