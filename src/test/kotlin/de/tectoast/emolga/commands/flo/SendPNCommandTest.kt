package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.TestCommandData
import de.tectoast.emolga.utils.Constants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SendPNCommandTest : FunSpec({
    test("SendPNCommand") {
        with(TestCommandData()) {
            SendPNCommand.exec(SendPNCommandArgs(Constants.FLOID, "Test :3"))
            awaitResponse().msg shouldBe "Done!"
        }
    }
})
