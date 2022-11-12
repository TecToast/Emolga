package de.tectoast.emolga.encryption

import de.tectoast.jsolf.JSONObject
import java.io.File
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object TokenEncrypter {

    fun decrypt(password: String): JSONObject {
        return with(Cipher.getInstance("AES/CBC/PKCS5PADDING")) {
            val lines = File("tokens.txt").readLines()
            init(Cipher.DECRYPT_MODE, run {
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val spec = PBEKeySpec(password.toCharArray(), password.toByteArray(), 65536, 256)
                SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
            }, IvParameterSpec(lines[1].toByteArray()))
            JSONObject(String(doFinal(Base64.getDecoder().decode(lines[0]))))
        }
    }
}
