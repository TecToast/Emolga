package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.awaitTimeout
import de.tectoast.emolga.testCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReplayCommandTest : FunSpec({
    test("ReplayCommand") {
        testCommand {
            val url = "https://replay.pokemonshowdown.com/gen9natdexdraft-1994163196"
            ReplayCommand.exec { this.url = url }
            responseDeferred.awaitTimeout().msg shouldBe url
        }
    }
})
