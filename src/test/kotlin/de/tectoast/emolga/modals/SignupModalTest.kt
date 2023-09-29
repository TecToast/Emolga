package de.tectoast.emolga.modals

import de.tectoast.emolga.utils.json.initMongo
import io.kotest.core.spec.style.FunSpec

class SignupModalTest : FunSpec({
    beforeSpec {
        initMongo("mongodb://localhost:27017/", "emolgatest")
    }
    test("SignupFast") {

    }
})
