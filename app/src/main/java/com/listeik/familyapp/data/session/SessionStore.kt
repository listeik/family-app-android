package com.listeik.familyapp.data.session

import android.content.Context
import androidx.core.content.edit
import com.listeik.familyapp.data.model.FamilySession

class SessionStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("family_session", Context.MODE_PRIVATE)

    fun load(userId: String): FamilySession? {
        val familyId = preferences.getString(KEY_FAMILY_ID, null) ?: return null
        val familyName = preferences.getString(KEY_FAMILY_NAME, null) ?: return null
        val inviteCode = preferences.getString(KEY_INVITE_CODE, null) ?: return null
        val userName = preferences.getString(KEY_USER_NAME, null) ?: return null
        return FamilySession(
            familyId = familyId,
            familyName = familyName,
            inviteCode = inviteCode,
            userId = userId,
            userName = userName,
        )
    }

    fun save(session: FamilySession) {
        preferences.edit {
            putString(KEY_FAMILY_ID, session.familyId)
            putString(KEY_FAMILY_NAME, session.familyName)
            putString(KEY_INVITE_CODE, session.inviteCode)
            putString(KEY_USER_NAME, session.userName)
        }
    }

    fun clear() {
        preferences.edit { clear() }
    }

    private companion object {
        const val KEY_FAMILY_ID = "family_id"
        const val KEY_FAMILY_NAME = "family_name"
        const val KEY_INVITE_CODE = "invite_code"
        const val KEY_USER_NAME = "user_name"
    }
}
