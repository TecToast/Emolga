package de.tectoast.emolga.ktor

import io.kotest.core.spec.style.FunSpec
import io.kotest.mpp.env
import kotlinx.coroutines.delay

class KtorTest : FunSpec({
    test("KtorTest") {
        Ktor.devMode = true
        Ktor.oauth2Secret = env("DISCORDOAUTHSECRET")!!
        Ktor.start()
        delay(5000)
        subscribeToYTChannel("UC-cffuneYaxrbNo8FmmmsLQ")
        readln()
        Ktor.server?.stop(1000, 1000)
    }
})
