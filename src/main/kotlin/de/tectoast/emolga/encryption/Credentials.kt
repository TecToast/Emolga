package de.tectoast.emolga.encryption

import de.tectoast.emolga.ktor.Ktor
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.json.Tokens
import de.tectoast.emolga.utils.myJSON
import mu.KotlinLogging
import java.io.File

object Credentials {
    lateinit var tokens: Tokens
    private val logger = KotlinLogging.logger {}
    fun load() {
        this.tokens = myJSON.decodeFromString(File(System.getenv("CONFIG_FILE") ?: "config.json").readText())
        injectTokens()
    }

    private fun injectTokens() {
        with(tokens.google) {
            Google.setCredentials(refreshtoken, clientid, clientsecret)
        }
        Ktor.oauth2Secret = tokens.oauth2.clientsecret
    }
}
