package de.tectoast.emolga.credentials

import de.tectoast.emolga.ktor.Ktor
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.StaticCloud
import de.tectoast.emolga.utils.json.Tokens
import de.tectoast.emolga.utils.myJSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Credentials {
    lateinit var tokens: Tokens
    suspend fun load() {
        this.tokens = myJSON.decodeFromString(withContext(Dispatchers.IO) {
            File(System.getenv("CONFIG_FILE") ?: "config.json").readText()
        })
        injectTokens()
    }

    private fun injectTokens() {
        with(tokens.google) {
            Google.setCredentials(refreshtoken, clientid, clientsecret)
        }
        with(tokens.staticCloud) {
            StaticCloud.init(token, baseUrl, hashLength)
        }
        Ktor.oauth2Secret = tokens.oauth2.clientsecret
        Ktor.artworkPath = tokens.artworkPath
    }
}
