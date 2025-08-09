package de.tectoast.emolga.ktor

import de.tectoast.emolga.utils.json.initMongo
import io.kotest.common.env
import io.kotest.core.spec.style.FunSpec

class KtorTest : FunSpec({
    test("KtorTest") {
        initMongo(env("MONGODB_URL")!!)
        Ktor.devMode = true
        Ktor.oauth2Secret = env("DISCORDOAUTHSECRET")!!
        Ktor.start(withYT = false)
        readln()
        Ktor.server?.stop(1000, 1000)
    }
})
