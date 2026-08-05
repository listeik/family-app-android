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
import com.listeik.familyapp.data.model.FamilyMember
import com.listeik.familyapp.data.model.FamilyMessage
import com.listeik.familyapp.data.model.FamilySecurityState
import com.listeik.familyapp.data.model.FamilySession
import com.listeik.familyapp.data.model.ItemCategory
import com.listeik.familyapp.data.model.ItemStatus
import com.listeik.familyapp.data.security.FamilyCipher
import com.listeik.familyapp.data.security.FamilyKeyStore
import com.listeik.familyapp.data.security.SecureInvite
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
    private val familyKeyStore = FamilyKeyStore(context)
    private val familyCipher = FamilyCipher()

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
        val familyKey = familyCipher.generateKey()
        val keyCheck = familyCipher.keyCheck(familyKey)
        val now = FieldValue.serverTimestamp()

        val batch = db.batch()
        batch.set(
            inviteRef,
            mapOf(
                "code" to inviteCode,
                "familyId" to familyRef.id,
                "createdBy" to userId,
                "encryptionVersion" to FamilyCipher.ENCRYPTION_VERSION,
                "keyCheck" to keyCheck,
                "createdAt" to now,
            ),
        )
        batch.set(
            familyRef,
            mapOf(
                "name" to encryptField(
                    familyKey,
                    familyRef.id,
                    COLLECTION_FAMILIES,
                    familyRef.id,
                    "name",
                    familyName.trim(),
                ),
                "inviteCode" to inviteCode,
                "createdBy" to userId,
                "encryptionVersion" to FamilyCipher.ENCRYPTION_VERSION,
                "keyCheck" to keyCheck,
                "createdAt" to now,
                "updatedAt" to now,
            ),
        )
        batch.set(
            memberRef,
            memberData(
                familyId = familyRef.id,
                userId = userId,
                userName = userName,
                inviteCode = inviteCode,
                familyKey = familyKey,
            ),
        )
        batch.commit().await()
        familyKeyStore.save(familyRef.id, familyKey)

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

    override suspend fun joinFamily(secureInvite: String, userName: String): FamilySession {
        val userId = ensureSignedIn()
        val parsedInvite = SecureInvite.parse(secureInvite)
        val normalizedCode = parsedInvite.inviteCode
        val invite = db.collection(COLLECTION_INVITES).document(normalizedCode).get().await()
        val familyId = invite.getString("familyId")
            ?: error("Семья с таким кодом не найдена")
        val expectedKeyCheck = invite.getString("keyCheck")
            ?: error("Создатель семьи еще не включил защищенные приглашения")
        require(familyCipher.keyCheck(parsedInvite.familyKey) == expectedKeyCheck) {
            "Защищенное приглашение не подходит к этой семье"
        }
        val familyRef = db.collection(COLLECTION_FAMILIES).document(familyId)
        val memberRef = familyRef.collection(COLLECTION_MEMBERS).document(userId)

        memberRef.set(
            memberData(
                familyId = familyId,
                userId = userId,
                userName = userName,
                inviteCode = normalizedCode,
                familyKey = parsedInvite.familyKey,
            ),
        ).await()
        val family = familyRef.get().await()
        val familyName = decryptField(
            parsedInvite.familyKey,
            familyId,
            COLLECTION_FAMILIES,
            familyId,
            "name",
            family.getString("name") ?: "Семья",
        )

        val session = FamilySession(
            familyId = familyId,
            familyName = familyName,
            inviteCode = normalizedCode,
            userId = userId,
            userName = userName.trim(),
        )
        familyKeyStore.save(familyId, parsedInvite.familyKey)
        sessionStore.save(session)
        saveTokenIfAvailable(session.familyId)
        addEvent(session, "${session.userName} присоединился к семье", null)
        return session
    }

    override suspend fun getSecurityState(session: FamilySession): FamilySecurityState {
        val family = familyDoc(session.familyId).get().await()
        val isEnabled =
            (family.getLong("encryptionVersion") ?: 0L) >= FamilyCipher.ENCRYPTION_VERSION
        val expectedKeyCheck = family.getString("keyCheck")
        val localKey = familyKeyStore.load(session.familyId)
        val hasValidLocalKey = localKey != null &&
            (!isEnabled || expectedKeyCheck == familyCipher.keyCheck(localKey))
        if (localKey != null && !hasValidLocalKey) {
            familyKeyStore.remove(session.familyId)
        }
        return FamilySecurityState(
            isEnabled = isEnabled,
            hasLocalKey = hasValidLocalKey,
            canEnable = !isEnabled && family.getString("createdBy") == session.userId,
        )
    }

    override suspend fun enableEncryption(session: FamilySession) {
        val familyRef = familyDoc(session.familyId)
        val family = familyRef.get().await()
        require(family.getString("createdBy") == session.userId) {
            "Включить защиту может создатель семьи"
        }
        require((family.getLong("encryptionVersion") ?: 0L) < FamilyCipher.ENCRYPTION_VERSION) {
            "Защита семьи уже включена. Импортируйте защищенное приглашение"
        }

        val familyKey = familyKeyStore.load(session.familyId) ?: familyCipher.generateKey()
        val keyCheck = familyCipher.keyCheck(familyKey)
        familyKeyStore.save(session.familyId, familyKey)
        migrateLegacyContent(session, familyKey)

        val inviteRef = db.collection(COLLECTION_INVITES).document(session.inviteCode)
        val batch = db.batch()
        batch.update(
            inviteRef,
            mapOf(
                "encryptionVersion" to FamilyCipher.ENCRYPTION_VERSION,
                "keyCheck" to keyCheck,
            ),
        )
        batch.update(
            familyRef,
            mapOf(
                "name" to encryptField(
                    familyKey,
                    session.familyId,
                    COLLECTION_FAMILIES,
                    session.familyId,
                    "name",
                    family.getString("name") ?: session.familyName,
                ),
                "encryptionVersion" to FamilyCipher.ENCRYPTION_VERSION,
                "keyCheck" to keyCheck,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.commit().await()
        migrateLegacyContent(session, familyKey)
    }

    override suspend fun importSecurityKey(session: FamilySession, secureInvite: String) {
        val parsedInvite = SecureInvite.parse(secureInvite)
        require(parsedInvite.inviteCode == session.inviteCode) {
            "Это приглашение от другой семьи"
        }
        val family = familyDoc(session.familyId).get().await()
        val expectedKeyCheck = family.getString("keyCheck")
            ?: error("Защита этой семьи еще не включена")
        require(familyCipher.keyCheck(parsedInvite.familyKey) == expectedKeyCheck) {
            "Защищенное приглашение не подходит к этой семье"
        }
        familyKeyStore.save(session.familyId, parsedInvite.familyKey)
    }

    override fun getSecureInvite(session: FamilySession): String? =
        familyKeyStore.load(session.familyId)?.let {
            SecureInvite.create(session.inviteCode, it)
        }

    override suspend fun leaveFamily(session: FamilySession) {
        familyDoc(session.familyId)
            .collection(COLLECTION_MEMBERS)
            .document(session.userId)
            .delete()
            .await()
        familyKeyStore.remove(session.familyId)
        sessionStore.clear()
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
        val familyKey = requireFamilyKey(session.familyId)
        val cleanPortions = portions?.coerceIn(1, 100)
        val data = mutableMapOf<String, Any?>(
            "id" to itemRef.id,
            "familyId" to session.familyId,
            "title" to encryptField(
                familyKey,
                session.familyId,
                COLLECTION_ITEMS,
                itemRef.id,
                "title",
                title.trim(),
            ),
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

    override suspend fun adjustFoodPortions(
        session: FamilySession,
        item: FamilyItem,
        delta: Int,
    ) {
        require(item.category == ItemCategory.FOOD && delta in setOf(-1, 1))
        val itemRef = familyDoc(session.familyId).collection(COLLECTION_ITEMS).document(item.id)
        val eventRef = familyDoc(session.familyId).collection(COLLECTION_EVENTS).document()
        val familyKey = requireFamilyKey(session.familyId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(itemRef)
            val total = snapshot.getLong("totalPortions")?.toInt()?.coerceIn(1, 100)
                ?: return@runTransaction Unit
            val current = snapshot.getLong("remainingPortions")?.toInt()?.coerceIn(0, total)
                ?: return@runTransaction Unit
            val next = (current + delta).coerceIn(0, total)
            if (next == current) return@runTransaction Unit

            val nextStatus = when (next) {
                0 -> ItemStatus.FINISHED
                total -> ItemStatus.READY
                else -> ItemStatus.IN_PROGRESS
            }
            val action = if (delta < 0) "съел порцию" else "вернул порцию"
            val eventText =
                "${session.userName} $action: ${item.title} · осталось $next из $total".take(240)

            transaction.update(
                itemRef,
                mapOf(
                    "remainingPortions" to next,
                    "status" to nextStatus.name,
                    "updatedBy" to session.userId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            transaction.set(
                eventRef,
                mapOf(
                    "id" to eventRef.id,
                    "familyId" to session.familyId,
                    "actorId" to session.userId,
                    "text" to encryptField(
                        familyKey,
                        session.familyId,
                        COLLECTION_EVENTS,
                        eventRef.id,
                        "text",
                        eventText,
                    ),
                    "itemId" to item.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
        }.await()
    }

    override suspend fun setItemCompleted(
        session: FamilySession,
        item: FamilyItem,
        completed: Boolean,
    ) {
        val targetStatus = if (completed) {
            when (item.category) {
                ItemCategory.FOOD -> ItemStatus.FINISHED
                ItemCategory.BUY -> ItemStatus.BOUGHT
                ItemCategory.TASK -> ItemStatus.DONE
                ItemCategory.WISH -> ItemStatus.ARCHIVED
            }
        } else {
            item.category.defaultStatus
        }
        if (targetStatus == item.status) return

        val itemRef = familyDoc(session.familyId).collection(COLLECTION_ITEMS).document(item.id)
        val eventRef = familyDoc(session.familyId).collection(COLLECTION_EVENTS).document()
        val familyKey = requireFamilyKey(session.familyId)
        val eventText = (if (completed) {
            "${session.userName} завершил: ${item.title}"
        } else {
            "${session.userName} вернул в список: ${item.title}"
        }).take(240)
        val batch = db.batch()
        batch.update(
            itemRef,
            mapOf(
                "status" to targetStatus.name,
                "updatedBy" to session.userId,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.set(
            eventRef,
            mapOf(
                "id" to eventRef.id,
                "familyId" to session.familyId,
                "actorId" to session.userId,
                "text" to encryptField(
                    familyKey,
                    session.familyId,
                    COLLECTION_EVENTS,
                    eventRef.id,
                    "text",
                    eventText,
                ),
                "itemId" to item.id,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.commit().await()
    }

    override suspend fun deleteItem(session: FamilySession, item: FamilyItem) {
        familyDoc(session.familyId).collection(COLLECTION_ITEMS).document(item.id).delete().await()
        addEvent(session, "${session.userName} удалил: ${item.title}", item.id)
    }

    override suspend fun sendMessage(session: FamilySession, text: String, itemId: String?) {
        val messageRef = familyDoc(session.familyId).collection(COLLECTION_MESSAGES).document()
        val familyKey = requireFamilyKey(session.familyId)
        messageRef.set(
            mapOf(
                "id" to messageRef.id,
                "familyId" to session.familyId,
                "senderId" to session.userId,
                "senderName" to encryptField(
                    familyKey,
                    session.familyId,
                    COLLECTION_MESSAGES,
                    messageRef.id,
                    "senderName",
                    session.userName,
                ),
                "text" to encryptField(
                    familyKey,
                    session.familyId,
                    COLLECTION_MESSAGES,
                    messageRef.id,
                    "text",
                    text.trim(),
                ),
                "itemId" to itemId,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override fun observeItems(familyId: String): Flow<List<FamilyItem>> {
        val familyKey = requireFamilyKey(familyId)
        return familyDoc(familyId)
            .collection(COLLECTION_ITEMS)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .asFlow { snapshot ->
                snapshot.documents.mapNotNull { it.toFamilyItem(familyId, familyKey) }
            }
    }

    override fun observeMembers(familyId: String): Flow<List<FamilyMember>> {
        val familyKey = requireFamilyKey(familyId)
        return familyDoc(familyId)
            .collection(COLLECTION_MEMBERS)
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .asFlow { snapshot ->
                snapshot.documents.mapNotNull { it.toFamilyMember(familyId, familyKey) }
            }
    }

    override fun observeEvents(familyId: String): Flow<List<ActivityEvent>> {
        val familyKey = requireFamilyKey(familyId)
        return familyDoc(familyId)
            .collection(COLLECTION_EVENTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .asFlow { snapshot ->
                snapshot.documents.mapNotNull { it.toActivityEvent(familyId, familyKey) }
            }
    }

    override fun observeMessages(familyId: String): Flow<List<FamilyMessage>> {
        val familyKey = requireFamilyKey(familyId)
        return familyDoc(familyId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .asFlow { snapshot ->
                snapshot.documents.mapNotNull { it.toFamilyMessage(familyId, familyKey) }.reversed()
            }
    }

    private suspend fun saveTokenIfAvailable(familyId: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            saveMessagingToken(familyId, token)
        }
    }

    private suspend fun addEvent(session: FamilySession, text: String, itemId: String?) {
        val eventRef = familyDoc(session.familyId).collection(COLLECTION_EVENTS).document()
        val familyKey = requireFamilyKey(session.familyId)
        eventRef.set(
            mapOf(
                "id" to eventRef.id,
                "familyId" to session.familyId,
                "actorId" to session.userId,
                "text" to encryptField(
                    familyKey,
                    session.familyId,
                    COLLECTION_EVENTS,
                    eventRef.id,
                    "text",
                    text,
                ),
                "itemId" to itemId,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun memberData(
        familyId: String,
        userId: String,
        userName: String,
        inviteCode: String,
        familyKey: ByteArray,
    ): Map<String, Any?> =
        mapOf(
            "uid" to userId,
            "name" to encryptField(
                familyKey,
                familyId,
                COLLECTION_MEMBERS,
                userId,
                "name",
                userName.trim(),
            ),
            "avatarColor" to avatarColorFor(userName),
            "inviteCode" to inviteCode,
            "fcmToken" to null,
            "joinedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

    private fun familyDoc(familyId: String) =
        db.collection(COLLECTION_FAMILIES).document(familyId)

    private suspend fun migrateLegacyContent(session: FamilySession, familyKey: ByteArray) {
        migrateCollection(session, familyKey, COLLECTION_MEMBERS, listOf("name"))
        migrateCollection(session, familyKey, COLLECTION_ITEMS, listOf("title"))
        migrateCollection(session, familyKey, COLLECTION_EVENTS, listOf("text"))
        migrateCollection(
            session,
            familyKey,
            COLLECTION_MESSAGES,
            listOf("senderName", "text"),
        )
    }

    private suspend fun migrateCollection(
        session: FamilySession,
        familyKey: ByteArray,
        collection: String,
        fields: List<String>,
    ) {
        val snapshot = familyDoc(session.familyId).collection(collection).get().await()
        snapshot.documents.chunked(MIGRATION_BATCH_SIZE).forEach { documents ->
            val batch = db.batch()
            var writeCount = 0
            documents.forEach { document ->
                val updates = fields.mapNotNull { field ->
                    val value = document.getString(field) ?: return@mapNotNull null
                    if (familyCipher.isEncrypted(value)) return@mapNotNull null
                    field to encryptField(
                        familyKey,
                        session.familyId,
                        collection,
                        document.id,
                        field,
                        value,
                    )
                }.toMap().toMutableMap<String, Any>()
                if (updates.isEmpty()) return@forEach
                if (collection == COLLECTION_ITEMS) {
                    updates["updatedBy"] = session.userId
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                }
                batch.update(document.reference, updates)
                writeCount += 1
            }
            if (writeCount > 0) batch.commit().await()
        }
    }

    private fun requireFamilyKey(familyId: String): ByteArray =
        familyKeyStore.load(familyId)
            ?: error("На этом устройстве нет ключа семьи. Импортируйте защищенное приглашение")

    private fun encryptField(
        familyKey: ByteArray,
        familyId: String,
        collection: String,
        documentId: String,
        field: String,
        value: String,
    ): String = familyCipher.encrypt(
        familyKey,
        value,
        FamilyCipher.aad(familyId, collection, documentId, field),
    )

    private fun decryptField(
        familyKey: ByteArray,
        familyId: String,
        collection: String,
        documentId: String,
        field: String,
        value: String,
    ): String = familyCipher.decrypt(
        familyKey,
        value,
        FamilyCipher.aad(familyId, collection, documentId, field),
    )

    private fun DocumentSnapshot.toFamilyItem(
        familyId: String,
        familyKey: ByteArray,
    ): FamilyItem? =
        runCatching {
            FamilyItem(
                id = getString("id") ?: id,
                familyId = familyId,
                title = decryptField(
                    familyKey,
                    familyId,
                    COLLECTION_ITEMS,
                    id,
                    "title",
                    getString("title").orEmpty(),
                ),
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

    private fun DocumentSnapshot.toFamilyMember(
        familyId: String,
        familyKey: ByteArray,
    ): FamilyMember? =
        runCatching {
            FamilyMember(
                uid = getString("uid") ?: id,
                name = decryptField(
                    familyKey,
                    familyId,
                    COLLECTION_MEMBERS,
                    id,
                    "name",
                    getString("name").orEmpty(),
                ),
                avatarColor = getString("avatarColor") ?: "#587060",
                joinedAtMillis = timestampMillis("joinedAt"),
            )
        }.getOrNull()

    private fun DocumentSnapshot.toActivityEvent(
        familyId: String,
        familyKey: ByteArray,
    ): ActivityEvent? =
        runCatching {
            ActivityEvent(
                id = getString("id") ?: id,
                familyId = familyId,
                actorId = getString("actorId").orEmpty(),
                text = decryptField(
                    familyKey,
                    familyId,
                    COLLECTION_EVENTS,
                    id,
                    "text",
                    getString("text").orEmpty(),
                ),
                itemId = getString("itemId"),
                createdAtMillis = timestampMillis("createdAt"),
            )
        }.getOrNull()

    private fun DocumentSnapshot.toFamilyMessage(
        familyId: String,
        familyKey: ByteArray,
    ): FamilyMessage? =
        runCatching {
            FamilyMessage(
                id = getString("id") ?: id,
                familyId = familyId,
                senderId = getString("senderId").orEmpty(),
                senderName = decryptField(
                    familyKey,
                    familyId,
                    COLLECTION_MESSAGES,
                    id,
                    "senderName",
                    getString("senderName").orEmpty(),
                ),
                text = decryptField(
                    familyKey,
                    familyId,
                    COLLECTION_MESSAGES,
                    id,
                    "text",
                    getString("text").orEmpty(),
                ),
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
                    runCatching { mapper(snapshot) }
                        .onSuccess { trySend(it) }
                        .onFailure { close(it) }
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
        const val MIGRATION_BATCH_SIZE = 400

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
            return colors[Math.floorMod(seed.hashCode(), colors.size)]
        }
    }
}
