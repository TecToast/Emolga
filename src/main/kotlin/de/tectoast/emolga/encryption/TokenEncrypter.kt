package de.tectoast.emolga.encryption

import de.tectoast.emolga.commands.myJSON
import de.tectoast.emolga.utils.json.Tokens
import java.io.File
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.measureTimeMillis

object TokenEncrypter {

    fun decrypt(password: String): Tokens {
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
        return tokens
    }
}
