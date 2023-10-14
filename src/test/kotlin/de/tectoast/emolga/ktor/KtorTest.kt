package de.tectoast.emolga.ktor

import io.kotest.core.spec.style.FunSpec
import io.kotest.mpp.env

class KtorTest : FunSpec({
    test("KtorTest") {
        Ktor.devMode = true
        Ktor.oauth2Secret = env("DISCORDOAUTHSECRET")!!
        Ktor.start()
        readln()
        Ktor.server?.stop(1000, 1000)
    }
})
