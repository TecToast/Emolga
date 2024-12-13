package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.TestInteractionData
import de.tectoast.emolga.utils.Constants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SendPNCommandTest : FunSpec({
    xtest("SendPNCommand") {
        with(TestInteractionData()) {
            SendFeatures.SendPNCommand.exec { this.id = Constants.FLOID; this.msg = "Test :3" }
            awaitResponse().msg shouldBe "Done!"
        }
    }
})
