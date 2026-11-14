package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BroadcastNotice
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.FeedPost
import com.example.data.model.ModerationSettings
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.UserAccount
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.data.util.unwrapFirestoreException
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialChatRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun isBootstrappedAdminEmail(email: String?, isVerified: Boolean): Boolean {
    if (!isVerified || email.isNullOrBlank()) return false
    return email.lowercase() in BOOTSTRAPPED_ADMIN_EMAILS
  }

  suspend fun syncSignedInUserProfile(
    uid: String,
    email: String,
    displayName: String,
    photoUrl: String?,
    isEmailVerified: Boolean,
  ): Result<Unit> = runCatching {
    val safeName = displayName.ifBlank { email.substringBefore("@").ifBlank { "ব্যবহারকারী" } }.take(80)
    val safeEmail = email.ifBlank { "$uid@socialchat.local" }.take(200)
    val safePhoto = photoUrl?.take(500)?.takeIf { it.isNotBlank() }

    val userRef = db.collection(USERS).document(uid)
    val existingUser = userRef.get().await()
    val isBootAdmin = isBootstrappedAdminEmail(safeEmail, isEmailVerified)
    val existingUserDisplayName = existingUser.getString("displayName")?.takeIf { it.isNotBlank() }
    val resolvedName = existingUserDisplayName ?: safeName
    if (!existingUser.exists()) {
      val createPayload =
        mapOf(
          "uid" to uid,
          "email" to safeEmail,
          "displayName" to safeName,
          "photoUrl" to safePhoto,
          "isBanned" to false,
          "isVerified" to isBootAdmin,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      userRef.set(createPayload).await()
    } else {
      val updatePayload =
        mapOf(
          "email" to safeEmail,
          "displayName" to resolvedName,
          "photoUrl" to safePhoto,
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      userRef.update(updatePayload).await()
    }

    val profileRef = db.collection(PUBLIC_PROFILES).document(uid)
    val existingProfile = profileRef.get().await()
    if (!existingProfile.exists()) {
      val createProfilePayload =
        mapOf(
          "uid" to uid,
          "displayName" to resolvedName,
          "photoUrl" to safePhoto,
          "bio" to "Connecta কমিউনিটি সদস্য",
          "isDiscoverable" to true,
          "profileVisibility" to "public",
          "isAdminBadge" to isBootAdmin,
          "isVerified" to isBootAdmin,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      profileRef.set(createProfilePayload).await()
    } else {
      val existingProfileName = existingProfile.getString("displayName")?.takeIf { it.isNotBlank() }
      val updateProfilePayload =
        mutableMapOf<String, Any>(
          "displayName" to (existingProfileName ?: resolvedName),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (safePhoto != null) updateProfilePayload["photoUrl"] = safePhoto
      if (isBootAdmin) updateProfilePayload["isAdminBadge"] = true
      profileRef.update(updateProfilePayload).await()
    }
    Unit
  }.onFailure { e ->
    if (e !is CancellationException) {
      handleFirestoreError(e, OperationType.WRITE, "$USERS/$uid", auth)
    }
  }

  fun observeCurrentUserAccount(uid: String): Flow<UserAccount?> {
    val path = "$USERS/$uid"
    return db.collection(USERS)
      .document(uid)
      .snapshots()
      .map { snap -> snap.toObject(UserAccount::class.java, ServerTimestampBehavior.ESTIMATE) }
      .catch { error ->
        handleStreamError(error, OperationType.GET, path, uid)
        throw error
      }
  }

  fun observeCurrentPublicProfile(uid: String): Flow<PublicProfile?> {
    val path = "$PUBLIC_PROFILES/$uid"
    return db.collection(PUBLIC_PROFILES)
      .document(uid)
      .snapshots()
      .map { snap -> snap.toObject(PublicProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
      .catch { error ->
        handleStreamError(error, OperationType.GET, path, uid)
        emit(null)
      }
  }

  suspend fun updateUserProfile(
    displayName: String,
    bio: String?,
    profileVisibility: String? = null,
  ): Result<Unit> {
    val uid = requireUserId()
    val safeName = displayName.trim().take(80)
    require(safeName.isNotBlank()) { "Display name cannot be blank" }
    val safeBio = bio?.trim()?.take(200)
    val safeVisibility = profileVisibility?.trim()?.lowercase()
    if (safeVisibility != null) {
      require(safeVisibility in VALID_PROFILE_VISIBILITIES) { "Invalid profile visibility" }
    }
    val userRef = db.collection(USERS).document(uid)
    val profileRef = db.collection(PUBLIC_PROFILES).document(uid)
    return try {
      val profileSnap = profileRef.get().await()
      val batch = db.batch()
      batch.update(
        userRef,
        mapOf(
          "displayName" to safeName,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      if (profileSnap.exists()) {
        val profileUpdates =
          mutableMapOf<String, Any>(
            "displayName" to safeName,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (safeBio != null) {
          profileUpdates["bio"] = safeBio
        }
        if (safeVisibility != null) {
          profileUpdates["profileVisibility"] = safeVisibility
          profileUpdates["isDiscoverable"] = (safeVisibility == "public")
        }
        batch.update(profileRef, profileUpdates)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeIsAdmin(uid: String, email: String?, isEmailVerified: Boolean): Flow<Boolean> {
    if (isBootstrappedAdminEmail(email, isEmailVerified)) {
      return flow { emit(true) }
    }
    val path = "$ADMINS/$uid"
    return db.collection(ADMINS)
      .document(uid)
      .snapshots()
      .map { snap -> snap.exists() }
      .catch { error ->
        handleStreamError(error, OperationType.GET, path, uid)
        emit(false)
      }
  }

  fun observePublicFeedPosts(): Flow<List<FeedPost>> = flow {
    val uid = requireUserId()
    val path = POSTS
    emitAll(
      db.collection(POSTS)
        .whereEqualTo("visibility", "public")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(FeedPost::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeAllPostsForAdmin(): Flow<List<FeedPost>> = flow {
    val uid = requireUserId()
    val path = POSTS
    emitAll(
      db.collection(POSTS)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(FeedPost::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun createPost(
    content: String,
    category: String,
    authorName: String,
    authorPhotoUrl: String?,
    authorVerified: Boolean = false,
  ): Result<String> {
    val uid = requireUserId()
    val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection(POSTS).document(postId)
    val payload =
      mapOf(
        "authorId" to uid,
        "authorName" to authorName.ifBlank { "ব্যবহারকারী" }.take(80),
        "authorPhotoUrl" to authorPhotoUrl?.take(500)?.takeIf { it.isNotBlank() },
        "authorVerified" to authorVerified,
        "content" to content.trim().take(1000),
        "category" to category.takeIf { it in setOf("status", "photo", "announcement") }.orEmpty().ifBlank { "status" },
        "visibility" to "public",
        "likesCount" to 0L,
        "commentsCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }
    return try {
      docRef.set(payload).await()
      Result.success(postId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun togglePostLike(postId: String): Result<Boolean> {
    val uid = requireUserId()
    val postRef = db.collection(POSTS).document(postId)
    val likeRef = postRef.collection(LIKES).document(uid)
    return try {
      val existingLike = likeRef.get().await()
      val batch = db.batch()
      val likedNow: Boolean
      if (existingLike.exists()) {
        batch.delete(likeRef)
        batch.update(
          postRef,
          mapOf(
            "likesCount" to FieldValue.increment(-1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        likedNow = false
      } else {
        batch.set(
          likeRef,
          mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
        )
        batch.update(
          postRef,
          mapOf(
            "likesCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        likedNow = true
      }
      batch.commit().await()
      Result.success(likedNow)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, likeRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun isPostLikedByUser(postId: String): Boolean {
    val uid = auth.currentUser?.uid ?: return false
    val likeRef = db.collection(POSTS).document(postId).collection(LIKES).document(uid)
    return try {
      likeRef.get().await().exists()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, likeRef.path, auth)
      false
    }
  }

  fun observeComments(postId: String): Flow<List<PostComment>> = flow {
    val uid = requireUserId()
    val path = "$POSTS/$postId/$COMMENTS"
    emitAll(
      db.collection(POSTS)
        .document(postId)
        .collection(COMMENTS)
        .whereEqualTo("postId", postId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(PostComment::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun addComment(postId: String, authorName: String, text: String): Result<String> {
    val uid = requireUserId()
    val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val postRef = db.collection(POSTS).document(postId)
    val commentRef = postRef.collection(COMMENTS).document(commentId)
    return try {
      val batch = db.batch()
      batch.set(
        commentRef,
        mapOf(
          "postId" to postId,
          "authorId" to uid,
          "authorName" to authorName.ifBlank { "ব্যবহারকারী" }.take(80),
          "text" to text.trim().take(500),
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        postRef,
        mapOf(
          "commentsCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(commentId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, commentRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deletePost(postId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(POSTS).document(postId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeDiscoverableProfiles(): Flow<List<PublicProfile>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(PUBLIC_PROFILES)
        .whereEqualTo("isDiscoverable", true)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(PublicProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayName.lowercase() }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, PUBLIC_PROFILES, uid)
          throw error
        }
    )
  }

  fun observeMyChats(uid: String): Flow<List<ChatRoom>> {
    val path = CHATS
    return db.collection(CHATS)
      .whereArrayContains("participantIds", uid)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(ChatRoom::class.java, ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        handleStreamError(error, OperationType.LIST, path, uid)
        throw error
      }
  }

  suspend fun createOrOpenPrivateChat(myName: String, partner: PublicProfile): Result<String> {
    val uid = requireUserId()
    val sortedPair = listOf(uid, partner.uid).distinct().sorted()
    val chatId =
      if (sortedPair.size == 1) "dm_${uid}_${uid}"
      else "dm_${sortedPair[0]}_${sortedPair[1]}"
    val chatRef = db.collection(CHATS).document(chatId)
    return try {
      val existing = chatRef.get().await()
      if (!existing.exists()) {
        val names = mapOf(uid to myName.ifBlank { "আমি" }.take(80), partner.uid to partner.displayName.take(80))
        val payload =
          mapOf(
            "title" to partner.displayName.ifBlank { "প্রাইভেট চ্যাট" }.take(80),
            "chatType" to "private",
            "participantIds" to sortedPair,
            "participantNames" to names,
            "createdBy" to uid,
            "lastMessageText" to "চ্যাট শুরু হয়েছে",
            "lastSenderId" to uid,
            "lastSenderName" to myName.ifBlank { "আমি" }.take(80),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        chatRef.set(payload).await()
      }
      Result.success(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, chatRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun createGroupChat(
    title: String,
    myName: String,
    selectedMembers: List<PublicProfile>,
  ): Result<String> {
    val uid = requireUserId()
    val chatId = "grp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val chatRef = db.collection(CHATS).document(chatId)
    val participantIds = (listOf(uid) + selectedMembers.map { it.uid }).distinct().take(25)
    val participantNames = buildMap {
      put(uid, myName.ifBlank { "আমি" }.take(80))
      selectedMembers.take(24).forEach { put(it.uid, it.displayName.take(80)) }
    }
    val payload =
      mapOf(
        "title" to title.trim().ifBlank { "গ্রুপ চ্যাট" }.take(80),
        "chatType" to "group",
        "participantIds" to participantIds,
        "participantNames" to participantNames,
        "createdBy" to uid,
        "lastMessageText" to "গ্রুপ তৈরি হয়েছে",
        "lastSenderId" to uid,
        "lastSenderName" to myName.ifBlank { "আমি" }.take(80),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      chatRef.set(payload).await()
      Result.success(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, chatRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> = flow {
    val uid = requireUserId()
    val path = "$CHATS/$chatId/$MESSAGES"
    emitAll(
      db.collection(CHATS)
        .document(chatId)
        .collection(MESSAGES)
        .whereArrayContains("participantIds", uid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun sendMessage(
    chatId: String,
    senderName: String,
    text: String,
    participantIds: List<String>,
  ): Result<String> {
    val uid = requireUserId()
    val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val chatRef = db.collection(CHATS).document(chatId)
    val msgRef = chatRef.collection(MESSAGES).document(msgId)
    val cleanText = text.trim().take(1000)
    val safeSender = senderName.ifBlank { "আমি" }.take(80)
    return try {
      val batch = db.batch()
      batch.set(
        msgRef,
        mapOf(
          "chatId" to chatId,
          "senderId" to uid,
          "senderName" to safeSender,
          "text" to cleanText,
          "participantIds" to participantIds.distinct().take(25),
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        chatRef,
        mapOf(
          "lastMessageText" to cleanText,
          "lastSenderId" to uid,
          "lastSenderName" to safeSender,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(msgId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeActiveBroadcasts(): Flow<List<BroadcastNotice>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(BROADCASTS)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(BroadcastNotice::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, BROADCASTS, uid)
          throw error
        }
    )
  }

  fun observeAllBroadcastsForAdmin(): Flow<List<BroadcastNotice>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(BROADCASTS)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(BroadcastNotice::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, BROADCASTS, uid)
          throw error
        }
    )
  }

  suspend fun publishBroadcast(
    title: String,
    message: String,
    priority: String,
  ): Result<String> {
    val uid = requireUserId()
    val broadcastId = "bc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection(BROADCASTS).document(broadcastId)
    val payload =
      mapOf(
        "title" to title.trim().take(120),
        "message" to message.trim().take(500),
        "priority" to priority.takeIf { it in setOf("info", "important", "urgent") }.orEmpty().ifBlank { "info" },
        "isActive" to true,
        "createdBy" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(broadcastId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun setBroadcastActive(broadcastId: String, active: Boolean): Result<Unit> {
    requireUserId()
    val docRef = db.collection(BROADCASTS).document(broadcastId)
    return try {
      docRef.update(mapOf("isActive" to active, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeModerationSettings(): Flow<ModerationSettings> = flow {
    val uid = requireUserId()
    val path = "$APP_SETTINGS/$MODERATION_DOC"
    emitAll(
      db.collection(APP_SETTINGS)
        .document(MODERATION_DOC)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(ModerationSettings::class.java, ServerTimestampBehavior.ESTIMATE)
              ?: ModerationSettings()
          } else {
            ModerationSettings()
          }
        }
        .catch { error ->
          handleStreamError(error, OperationType.GET, path, uid)
          emit(ModerationSettings())
        }
    )
  }

  suspend fun updateModerationSettings(settings: ModerationSettings): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection(APP_SETTINGS).document(MODERATION_DOC)
    val payload =
      mapOf(
        "allowNewPosts" to settings.allowNewPosts,
        "allowNewChats" to settings.allowNewChats,
        "autoFilterEnabled" to settings.autoFilterEnabled,
        "maintenanceNotice" to settings.maintenanceNotice.trim().take(300),
        "updatedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeAllUsersForAdmin(): Flow<List<UserAccount>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(USERS)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(UserAccount::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayName.lowercase() }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, USERS, uid)
          throw error
        }
    )
  }

  fun observeAdminIdsForAdmin(): Flow<Set<String>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(ADMINS)
        .snapshots()
        .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, ADMINS, uid)
          emit(emptySet())
        }
    )
  }

  suspend fun setUserBanned(targetUid: String, banned: Boolean): Result<Unit> {
    requireUserId()
    val docRef = db.collection(USERS).document(targetUid)
    return try {
      docRef.update(mapOf("isBanned" to banned, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun setUserVerified(targetUid: String, verified: Boolean): Result<Unit> {
    requireUserId()
    val userRef = db.collection(USERS).document(targetUid)
    val profileRef = db.collection(PUBLIC_PROFILES).document(targetUid)
    return try {
      val profileSnap = profileRef.get().await()
      val batch = db.batch()
      batch.update(
        userRef,
        mapOf("isVerified" to verified, "updatedAt" to FieldValue.serverTimestamp()),
      )
      if (profileSnap.exists()) {
        batch.update(
          profileRef,
          mapOf("isVerified" to verified, "updatedAt" to FieldValue.serverTimestamp()),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun setUserAdminRole(targetUid: String, makeAdmin: Boolean): Result<Unit> {
    val uid = requireUserId()
    val adminRef = db.collection(ADMINS).document(targetUid)
    val profileRef = db.collection(PUBLIC_PROFILES).document(targetUid)
    return try {
      if (makeAdmin) {
        adminRef
          .set(
            mapOf(
              "uid" to targetUid,
              "grantedBy" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        adminRef.delete().await()
      }
      val profileSnap = profileRef.get().await()
      if (profileSnap.exists()) {
        profileRef
          .update(
            mapOf(
              "isAdminBadge" to makeAdmin,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, adminRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun setPostVisibility(postId: String, visibility: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(POSTS).document(postId)
    return try {
      docRef
        .update(
          mapOf(
            "visibility" to visibility,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  private fun handleStreamError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    expectedUid: String,
  ) {
    val firestoreError = error.unwrapFirestoreException()
    if (firestoreError == null && error is CancellationException) return
    val toLog = firestoreError ?: error
    if (auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(toLog, operationType, path, auth)
    }
  }

  companion object {
    const val USERS = "users"
    const val PUBLIC_PROFILES = "public_profiles"
    const val ADMINS = "admins"
    const val POSTS = "posts"
    const val LIKES = "likes"
    const val COMMENTS = "comments"
    const val CHATS = "chats"
    const val MESSAGES = "messages"
    const val BROADCASTS = "broadcasts"
    const val APP_SETTINGS = "app_settings"
    const val MODERATION_DOC = "moderation"

    private val VALID_PROFILE_VISIBILITIES = setOf("public", "friends", "private")
    private val BOOTSTRAPPED_ADMIN_EMAILS = setOf("tasauf202@gmail.com", "admin@socialchat.com")
  }
}

private typealias ServerTimestampBehavior = DocumentSnapshot.ServerTimestampBehavior
