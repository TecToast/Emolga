package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.testCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReplayCommandTest : FunSpec({
    test("ReplayCommand") {
        testCommand {
            val url = "https://replay.pokemonshowdown.com/gen9natdexdraft-1994163196"
            ReplayCommand.exec(ReplayCommandArgs(url))
            response.msg shouldBe url
        }
    }
})
