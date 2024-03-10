package de.tectoast.emolga.features.draft

import de.tectoast.emolga.createChannel
import de.tectoast.emolga.defaultChannel
import de.tectoast.emolga.keepAlive
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.createCoroutineScope
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SignupManagerTest : FunSpec({
    test("StressTest") {
        val signupChannel = createChannel("signup")
        SignupManager.createSignup(
            defaultChannel.idLong,
            signupChannel.idLong,
            defaultChannel.idLong,
            16,
            null,
            false,
            "GuMo"
        )
        val scope = createCoroutineScope("SignupTest", Dispatchers.IO)
        delay(5000)
        repeat(20) {
            scope.launch {
                SignupManager.signupUser(Constants.G.MY, Constants.FLOID + 1 + it, null, "GuMo", null)
            }
        }
        keepAlive()
    }
})
