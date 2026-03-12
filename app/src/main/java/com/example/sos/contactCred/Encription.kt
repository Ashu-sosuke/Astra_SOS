package com.example.sos.contactCred

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val SECRET = "MY_SUPER_SECRET_32_BYTE_KEY"

    private fun getKey(): SecretKey {
        val keyBytes = SECRET.toByteArray().copyOf(32)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(text: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        return Base64.encodeToString(cipher.doFinal(text.toByteArray()), Base64.DEFAULT)
    }

    fun decrypt(encrypted: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, getKey())
        val decoded = Base64.decode(encrypted, Base64.DEFAULT)
        return String(cipher.doFinal(decoded))
    }

    fun hash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(text.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
