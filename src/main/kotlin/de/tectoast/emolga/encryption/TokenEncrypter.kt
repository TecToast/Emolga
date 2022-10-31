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

    fun decrypt(password: String, ivStr: String): JSONObject {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val key = run {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), password.toByteArray(), 65536, 256)
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        }
        val iv = ivStr.toByteArray()
        val spec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return JSONObject(String(cipher.doFinal(Base64.getDecoder().decode(File("tokens.txt").readText().trim()))))
    }
}
