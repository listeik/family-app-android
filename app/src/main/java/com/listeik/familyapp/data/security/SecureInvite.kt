package com.listeik.familyapp.data.security

import java.util.Base64

data class ParsedSecureInvite(
    val inviteCode: String,
    val familyKey: ByteArray,
)

object SecureInvite {
    private const val PREFIX = "FH1"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun create(inviteCode: String, familyKey: ByteArray): String {
        require(familyKey.size == FamilyCipher.KEY_SIZE_BYTES)
        return "$PREFIX.${inviteCode.trim().uppercase()}.${encoder.encodeToString(familyKey)}"
    }

    fun parse(value: String): ParsedSecureInvite {
        val normalized = value.trim()
        val parts = normalized.split('.')
        require(parts.size == 3 && parts[0] == PREFIX) {
            "Нужно полное защищенное приглашение, начинающееся с FH1"
        }
        val inviteCode = parts[1].uppercase()
        require(inviteCode.matches(Regex("[A-HJ-NP-Z2-9]{6}"))) {
            "Некорректный код семьи"
        }
        val key = runCatching { decoder.decode(parts[2]) }
            .getOrElse { throw IllegalArgumentException("Поврежденное защищенное приглашение") }
        require(key.size == FamilyCipher.KEY_SIZE_BYTES) {
            "Поврежденный ключ семьи"
        }
        return ParsedSecureInvite(inviteCode, key)
    }
}
