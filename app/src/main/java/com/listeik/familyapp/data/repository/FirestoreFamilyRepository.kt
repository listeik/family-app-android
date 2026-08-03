package com.listeik.familyapp.data.repository

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.listeik.familyapp.data.model.ActivityEvent
import com.listeik.familyapp.data.model.FamilyItem
import com.listeik.familyapp.data.model.FamilyMessage
import com.listeik.familyapp.data.model.FamilySession
import com.listeik.familyapp.data.model.ItemCategory
import com.listeik.familyapp.data.model.ItemStatus
import com.listeik.familyapp.data.session.SessionStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class FirestoreFamilyRepository(context: Context) : FamilyRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val sessionStore = SessionStore(context)

    override suspend fun ensureSignedIn(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return currentUser.uid
        }
        return auth.signInAnonymously().await().user?.uid
            ?: error("Firebase Auth did not return a user")
    }

    override fun loadSavedSession(userId: String): FamilySession? =
        sessionStore.load(userId)

    override suspend fun createFamily(familyName: String, userName: String): FamilySession {
        val userId = ensureSignedIn()
        val familyRef = db.collection(COLLECTION_FAMILIES).document()
        val inviteCode = generateInviteCode()
        val inviteRef = db.collection(COLLECTION_INVITES).document(inviteCode)
        val memberRef = familyRef.collection(COLLECTION_MEMBERS).document(userId)
        val now = FieldValue.serverTimestamp()

        val batch = db.batch()
        batch.set(
            inviteRef,
            mapOf(
                "code" to inviteCode,
                "familyId" to familyRef.id,
                "createdBy" to userId,
                "createdAt" to now,
            ),
        )
        batch.set(
            familyRef,
            mapOf(
                "name" to familyName.trim(),
                "inviteCode" to inviteCode,
                "createdBy" to userId,
                "createdAt" to now,
                "updatedAt" to now,
            ),
        )
        batch.set(
            memberRef,
            memberData(userId = userId, userName = userName, inviteCode = inviteCode),
        )
        batch.commit().await()

        val session = FamilySession(
            familyId = familyRef.id,
            familyName = familyName.trim(),
            inviteCode = inviteCode,
            userId = userId,
            userName = userName.trim(),
        )
        sessionStore.save(session)
        saveTokenIfAvailable(session.familyId)
        addEvent(session, "Создана семья ${session.familyName}", null)
        return session
    }

    override suspend fun joinFamily(inviteCode: String, userName: String): FamilySession {
        val userId = ensureSignedIn()
        val normalizedCode = inviteCode.trim().uppercase()
        val invite = db.collection(COLLECTION_INVITES).document(normalizedCode).get().await()
        val familyId = invite.getString("familyId")
            ?: error("Семья с таким кодом не найдена")
        val familyRef = db.collection(COLLECTION_FAMILIES).document(familyId)
        val memberRef = familyRef.collection(COLLECTION_MEMBERS).document(userId)

        memberRef.set(memberData(userId = userId, userName = userName, inviteCode = normalizedCode)).await()
        val family = familyRef.get().await()
        val familyName = family.getString("name") ?: "Семья"

        val session = FamilySession(
            familyId = familyId,
            familyName = familyName,
            inviteCode = normalizedCode,
            userId = userId,
            userName = userName.trim(),
        )
        sessionStore.save(session)
        saveTokenIfAvailable(session.familyId)
        addEvent(session, "${session.userName} присоединился к семье", null)
        return session
    }

    override suspend fun saveMessagingToken(familyId: String, token: String) {
        val userId = ensureSignedIn()
        db.collection(COLLECTION_FAMILIES)
            .document(familyId)
            .collection(COLLECTION_MEMBERS)
            .document(userId)
            .update(
                mapOf(
                    "fcmToken" to token,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    override suspend fun createItem(
        session: FamilySession,
        title: String,
        category: ItemCategory,
        portions: Int?,
    ) {
        val itemRef = familyDoc(session.familyId).collection(COLLECTION_ITEMS).document()
        val cleanPortions = portions?.coerceIn(1, 100)
        val data = mutableMapOf<String, Any?>(
            "id" to itemRef.id,
            "familyId" to session.familyId,
            "title" to title.trim(),
            "category" to category.name,
            "status" to category.defaultStatus.name,
            "createdBy" to session.userId,
            "updatedBy" to session.userId,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "totalPortions" to cleanPortions,
            "remainingPortions" to cleanPortions,
        )
        if (category != ItemCategory.FOOD) {
            data["totalPortions"] = null
            data["remainingPortions"] = null
        }
        itemRef.set(data).await()
        addEvent(session, "${session.userName}: ${category.label.lowercase()} — ${title.trim()}", itemRef.id)
    }

    override suspend fun moveItemForward(session: FamilySession, item: FamilyItem) {
        val nextStatus = item.status.nextFor(item.category)
        val itemRef = familyDoc(session.familyId).collection(COLLECTION_ITEMS).document(item.id)
        val updates = mutableMapOf<String, Any>(
            "status" to nextStatus.name,
            "updatedBy" to session.userId,
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        if (item.category == ItemCategory.FOOD && item.remainingPortions != null && item.remainingPortions > 0) {
            val nextRemaining = (item.remainingPortions - 1).coerceAtLeast(0)
            updates["remainingPortions"] = nextRemaining
            updates["status"] = if (nextRemaining == 0) ItemStatus.FINISHED.name else ItemStatus.IN_PROGRESS.name
        }

        itemRef.update(updates).await()
        val statusLabel = ItemStatus.valueOf(updates["status"] as String).label.lowercase()
        addEvent(session, "${session.userName}: ${item.title} теперь $statusLabel", item.id)
    }

    override suspend fun deleteItem(session: FamilySession, item: FamilyItem) {
        familyDoc(session.familyId).collection(COLLECTION_ITEMS).document(item.id).delete().await()
        addEvent(session, "${session.userName} удалил: ${item.title}", item.id)
    }

    override suspend fun sendMessage(session: FamilySession, text: String, itemId: String?) {
        val messageRef = familyDoc(session.familyId).collection(COLLECTION_MESSAGES).document()
        messageRef.set(
            mapOf(
                "id" to messageRef.id,
                "familyId" to session.familyId,
                "senderId" to session.userId,
                "senderName" to session.userName,
                "text" to text.trim(),
                "itemId" to itemId,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override fun observeItems(familyId: String): Flow<List<FamilyItem>> =
        familyDoc(familyId)
            .collection(COLLECTION_ITEMS)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .asFlow { snapshot -> snapshot.documents.mapNotNull { it.toFamilyItem() } }

    override fun observeEvents(familyId: String): Flow<List<ActivityEvent>> =
        familyDoc(familyId)
            .collection(COLLECTION_EVENTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .asFlow { snapshot -> snapshot.documents.mapNotNull { it.toActivityEvent() } }

    override fun observeMessages(familyId: String): Flow<List<FamilyMessage>> =
        familyDoc(familyId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .asFlow { snapshot -> snapshot.documents.mapNotNull { it.toFamilyMessage() }.reversed() }

    private suspend fun saveTokenIfAvailable(familyId: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            saveMessagingToken(familyId, token)
        }
    }

    private suspend fun addEvent(session: FamilySession, text: String, itemId: String?) {
        val eventRef = familyDoc(session.familyId).collection(COLLECTION_EVENTS).document()
        eventRef.set(
            mapOf(
                "id" to eventRef.id,
                "familyId" to session.familyId,
                "actorId" to session.userId,
                "text" to text,
                "itemId" to itemId,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun memberData(userId: String, userName: String, inviteCode: String): Map<String, Any?> =
        mapOf(
            "uid" to userId,
            "name" to userName.trim(),
            "avatarColor" to avatarColorFor(userName),
            "inviteCode" to inviteCode,
            "fcmToken" to null,
            "joinedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

    private fun familyDoc(familyId: String) =
        db.collection(COLLECTION_FAMILIES).document(familyId)

    private fun DocumentSnapshot.toFamilyItem(): FamilyItem? =
        runCatching {
            FamilyItem(
                id = getString("id") ?: id,
                familyId = getString("familyId").orEmpty(),
                title = getString("title").orEmpty(),
                category = enumValueOf(getString("category") ?: ItemCategory.TASK.name),
                status = enumValueOf(getString("status") ?: ItemStatus.TODO.name),
                createdBy = getString("createdBy").orEmpty(),
                updatedBy = getString("updatedBy").orEmpty(),
                createdAtMillis = timestampMillis("createdAt"),
                updatedAtMillis = timestampMillis("updatedAt"),
                totalPortions = getLong("totalPortions")?.toInt(),
                remainingPortions = getLong("remainingPortions")?.toInt(),
            )
        }.getOrNull()

    private fun DocumentSnapshot.toActivityEvent(): ActivityEvent? =
        runCatching {
            ActivityEvent(
                id = getString("id") ?: id,
                familyId = getString("familyId").orEmpty(),
                actorId = getString("actorId").orEmpty(),
                text = getString("text").orEmpty(),
                itemId = getString("itemId"),
                createdAtMillis = timestampMillis("createdAt"),
            )
        }.getOrNull()

    private fun DocumentSnapshot.toFamilyMessage(): FamilyMessage? =
        runCatching {
            FamilyMessage(
                id = getString("id") ?: id,
                familyId = getString("familyId").orEmpty(),
                senderId = getString("senderId").orEmpty(),
                senderName = getString("senderName").orEmpty(),
                text = getString("text").orEmpty(),
                itemId = getString("itemId"),
                createdAtMillis = timestampMillis("createdAt"),
            )
        }.getOrNull()

    private fun DocumentSnapshot.timestampMillis(field: String): Long =
        (getTimestamp(field) ?: Timestamp.now()).toDate().time

    private fun <T> Query.asFlow(mapper: (com.google.firebase.firestore.QuerySnapshot) -> T): Flow<T> =
        callbackFlow {
            val registration = addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(mapper(snapshot))
                }
            }
            awaitClose { registration.remove() }
        }

    private companion object {
        const val COLLECTION_FAMILIES = "families"
        const val COLLECTION_INVITES = "familyInvites"
        const val COLLECTION_MEMBERS = "members"
        const val COLLECTION_ITEMS = "items"
        const val COLLECTION_EVENTS = "events"
        const val COLLECTION_MESSAGES = "messages"

        fun generateInviteCode(): String {
            val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return buildString {
                repeat(6) {
                    append(alphabet[Random.nextInt(alphabet.length)])
                }
            }
        }

        fun avatarColorFor(seed: String): String {
            val colors = listOf("#2D6CDF", "#2E7D32", "#C62828", "#6A4C93", "#B26A00")
            return colors[seed.hashCode().let { kotlin.math.abs(it) } % colors.size]
        }
    }
}
