package de.tectoast.emolga.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TimeUtilsTest : FunSpec({
    test("parseShortTimeNew") {
        TimeUtils.parseShortTime("1d") shouldBe 86400
        TimeUtils.parseShortTime("2d") shouldBe 86400 * 2
        TimeUtils.parseShortTime("90m") shouldBe 90 * 60
        TimeUtils.parseShortTime("1m 30s") shouldBe 90
    }
})
