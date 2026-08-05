package com.listeik.familyapp.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class FamilyKeyStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(familyId: String, familyKey: ByteArray) {
        require(familyKey.size == FamilyCipher.KEY_SIZE_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        cipher.updateAAD(familyId.toByteArray(StandardCharsets.UTF_8))
        val payload = cipher.iv + cipher.doFinal(familyKey)
        preferences.edit {
            putString(keyFor(familyId), encoder.encodeToString(payload))
        }
    }

    fun load(familyId: String): ByteArray? {
        val stored = preferences.getString(keyFor(familyId), null) ?: return null
        return runCatching {
            val payload = decoder.decode(stored)
            require(payload.size > IV_SIZE_BYTES)
            val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
            val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
            cipher.updateAAD(familyId.toByteArray(StandardCharsets.UTF_8))
            cipher.doFinal(encrypted)
        }.getOrNull()
    }

    fun remove(familyId: String) {
        preferences.edit { remove(keyFor(familyId)) }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private fun keyFor(familyId: String) = "family_key_$familyId"

    private companion object {
        const val PREFERENCES_NAME = "family_security"
        const val MASTER_KEY_ALIAS = "family_hub_master_key_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val TAG_SIZE_BITS = 128
        val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        val decoder: Base64.Decoder = Base64.getUrlDecoder()
    }
}
