package com.listeik.familyapp.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyCipherTest {
    private val cipher = FamilyCipher()

    @Test
    fun encryptDecrypt_roundTripsUnicodeText() {
        val key = cipher.generateKey()
        val aad = FamilyCipher.aad("family", "messages", "message-1", "text")

        val encrypted = cipher.encrypt(key, "Папа купил молоко", aad)

        assertTrue(encrypted.startsWith(FamilyCipher.PREFIX))
        assertFalse(encrypted.contains("молоко"))
        assertEquals("Папа купил молоко", cipher.decrypt(key, encrypted, aad))
    }

    @Test
    fun encrypt_usesFreshNonceForEveryValue() {
        val key = cipher.generateKey()
        val aad = FamilyCipher.aad("family", "items", "item-1", "title")

        val first = cipher.encrypt(key, "Борщ", aad)
        val second = cipher.encrypt(key, "Борщ", aad)

        assertNotEquals(first, second)
    }

    @Test
    fun decrypt_rejectsCiphertextFromAnotherDocument() {
        val key = cipher.generateKey()
        val firstAad = FamilyCipher.aad("family", "messages", "message-1", "text")
        val secondAad = FamilyCipher.aad("family", "messages", "message-2", "text")
        val encrypted = cipher.encrypt(key, "Секрет", firstAad)

        assertThrows(Exception::class.java) {
            cipher.decrypt(key, encrypted, secondAad)
        }
    }

    @Test
    fun decrypt_keepsLegacyPlaintextReadableDuringMigration() {
        val key = cipher.generateKey()

        assertEquals("Старая запись", cipher.decrypt(key, "Старая запись", "legacy"))
    }

    @Test
    fun secureInvite_roundTripsFamilyKey() {
        val key = cipher.generateKey()

        val invite = SecureInvite.create("A7K9Q2", key)
        val parsed = SecureInvite.parse(invite)

        assertEquals("A7K9Q2", parsed.inviteCode)
        assertArrayEquals(key, parsed.familyKey)
    }

    @Test
    fun secureInvite_rejectsShortCodeWithoutKey() {
        assertThrows(IllegalArgumentException::class.java) {
            SecureInvite.parse("A7K9Q2")
        }
    }
}
