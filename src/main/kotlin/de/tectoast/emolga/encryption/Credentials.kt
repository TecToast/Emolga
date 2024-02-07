package de.tectoast.emolga.encryption

import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.json.Tokens
import de.tectoast.emolga.utils.myJSON
import java.io.File
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.measureTimeMillis

object Credentials {
    lateinit var tokens: Tokens
    fun load(password: String) {
        val tokens: Tokens
        println("MEASURE: " + measureTimeMillis {
            tokens = with(Cipher.getInstance("AES/CBC/PKCS5PADDING")) {
                val lines = File("tokens.txt").readLines()
                init(Cipher.DECRYPT_MODE, run {
                    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    val spec = PBEKeySpec(password.toCharArray(), password.toByteArray(), 65536, 256)
                    SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
                }, IvParameterSpec(lines[1].toByteArray()))
                myJSON.decodeFromString(String(doFinal(Base64.getDecoder().decode(lines[0]))))
            }
        })
        this.tokens = tokens
        injectTokens()
    }

    private fun injectTokens() {
        with(tokens.google) {
            Google.setCredentials(refreshtoken, clientid, clientsecret)
            Google.generateAccessToken()
        }
    }
}
