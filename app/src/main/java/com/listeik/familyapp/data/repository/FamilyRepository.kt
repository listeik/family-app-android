package com.listeik.familyapp.data.repository

import com.listeik.familyapp.data.model.ActivityEvent
import com.listeik.familyapp.data.model.FamilyItem
import com.listeik.familyapp.data.model.FamilyMember
import com.listeik.familyapp.data.model.FamilyMessage
import com.listeik.familyapp.data.model.FamilySecurityState
import com.listeik.familyapp.data.model.FamilySession
import com.listeik.familyapp.data.model.ItemCategory
import kotlinx.coroutines.flow.Flow

interface FamilyRepository {
    suspend fun ensureSignedIn(): String
    fun loadSavedSession(userId: String): FamilySession?
    suspend fun createFamily(familyName: String, userName: String): FamilySession
    suspend fun joinFamily(secureInvite: String, userName: String): FamilySession
    suspend fun getSecurityState(session: FamilySession): FamilySecurityState
    suspend fun enableEncryption(session: FamilySession)
    suspend fun importSecurityKey(session: FamilySession, secureInvite: String)
    fun getSecureInvite(session: FamilySession): String?
    suspend fun leaveFamily(session: FamilySession)
    suspend fun saveMessagingToken(familyId: String, token: String)
    suspend fun createItem(
        session: FamilySession,
        title: String,
        category: ItemCategory,
        portions: Int?,
    )

    suspend fun moveItemForward(session: FamilySession, item: FamilyItem)
    suspend fun adjustFoodPortions(session: FamilySession, item: FamilyItem, delta: Int)
    suspend fun setItemCompleted(session: FamilySession, item: FamilyItem, completed: Boolean)
    suspend fun deleteItem(session: FamilySession, item: FamilyItem)
    suspend fun sendMessage(session: FamilySession, text: String, itemId: String? = null)
    fun observeItems(familyId: String): Flow<List<FamilyItem>>
    fun observeMembers(familyId: String): Flow<List<FamilyMember>>
    fun observeEvents(familyId: String): Flow<List<ActivityEvent>>
    fun observeMessages(familyId: String): Flow<List<FamilyMessage>>
}
