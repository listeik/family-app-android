package com.listeik.familyapp.data.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class FamilyCipher(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun generateKey(): ByteArray = ByteArray(KEY_SIZE_BYTES).also(secureRandom::nextBytes)

    fun encrypt(key: ByteArray, plaintext: String, associatedData: String): String {
        require(key.size == KEY_SIZE_BYTES)
        val nonce = ByteArray(NONCE_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, KEY_ALGORITHM),
            GCMParameterSpec(TAG_SIZE_BITS, nonce),
        )
        cipher.updateAAD(associatedData.toByteArray(StandardCharsets.UTF_8))
        val encrypted = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        return PREFIX + encoder.encodeToString(nonce + encrypted)
    }

    fun decrypt(key: ByteArray, storedValue: String, associatedData: String): String {
        if (!isEncrypted(storedValue)) return storedValue
        require(key.size == KEY_SIZE_BYTES)
        val payload = decoder.decode(storedValue.removePrefix(PREFIX))
        require(payload.size > NONCE_SIZE_BYTES) { "Поврежденные зашифрованные данные" }
        val nonce = payload.copyOfRange(0, NONCE_SIZE_BYTES)
        val encrypted = payload.copyOfRange(NONCE_SIZE_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, KEY_ALGORITHM),
            GCMParameterSpec(TAG_SIZE_BITS, nonce),
        )
        cipher.updateAAD(associatedData.toByteArray(StandardCharsets.UTF_8))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    fun keyCheck(key: ByteArray): String {
        require(key.size == KEY_SIZE_BYTES)
        val digest = MessageDigest.getInstance("SHA-256").digest(KEY_CHECK_CONTEXT + key)
        return encoder.encodeToString(digest)
    }

    fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    companion object {
        const val ENCRYPTION_VERSION = 1L
        const val KEY_SIZE_BYTES = 32
        const val PREFIX = "e2e1:"
        private const val NONCE_SIZE_BYTES = 12
        private const val TAG_SIZE_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private val KEY_CHECK_CONTEXT = "FamilyHub key check v1".toByteArray(StandardCharsets.UTF_8)
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()

        fun aad(familyId: String, collection: String, documentId: String, field: String): String =
            listOf("familyhub", "v1", familyId, collection, documentId, field).joinToString("|")
    }
}
