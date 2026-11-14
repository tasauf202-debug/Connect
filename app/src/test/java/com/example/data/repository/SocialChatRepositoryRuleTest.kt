package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PublicProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun syncProfileAndCreatePost_authenticatedUser_succeedsAndEmitsInFeed() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = SocialChatRepository(firestore, auth)

    val syncResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.syncSignedInUserProfile(uid, ALICE_EMAIL, "Alice", null, isEmailVerified = true)
      }
    assertTrue(syncResult.isSuccess)

    val postResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createPost("হ্যালো কমিউনিটি ${UUID.randomUUID()}", "status", "Alice", null)
      }
    assertTrue(postResult.isSuccess)
    val postId = postResult.getOrThrow()

    val posts =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePublicFeedPosts().first { list -> list.any { it.id == postId } }
      }
    assertTrue(posts.any { it.id == postId && it.authorId == uid })

    val updateRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.updateUserProfile("Alice Updated", "Connecta Verified Explorer", "friends")
      }
    assertTrue(updateRes.isSuccess)
    val updatedAccount =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCurrentUserAccount(uid).first { it?.displayName == "Alice Updated" }
      }
    assertEquals("Alice Updated", updatedAccount?.displayName)
    val updatedProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo
          .observeCurrentPublicProfile(uid)
          .first { it?.displayName == "Alice Updated" && it.profileVisibility == "friends" }
      }
    assertEquals("Connecta Verified Explorer", updatedProfile?.bio)
    assertEquals("friends", updatedProfile?.profileVisibility)
    assertEquals(false, updatedProfile?.isDiscoverable)

    val privateRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.updateUserProfile("Alice Updated", "Connecta Verified Explorer", "private")
      }
    assertTrue(privateRes.isSuccess)
    val privateProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCurrentPublicProfile(uid).first { it?.profileVisibility == "private" }
      }
    assertEquals("private", privateProfile?.profileVisibility)
    assertEquals(false, privateProfile?.isDiscoverable)

    // Another user cannot read Alice's profile directly when profileVisibility == "private"
    signInTestUser(BOB_EMAIL)
    var privateDenied: FirebaseFirestoreException.Code? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("public_profiles").document(uid).get().await()
      }
    } catch (e: FirebaseFirestoreException) {
      privateDenied = e.code
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, privateDenied)
  }

  @Test
  fun toggleLikeAndAddComment_atomicUpdates_succeed() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = SocialChatRepository(firestore, auth)
    repo.syncSignedInUserProfile(aliceUid, ALICE_EMAIL, "Alice", null, isEmailVerified = true)
    val postId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createPost("টেস্ট পোস্ট", "status", "Alice", null).getOrThrow()
      }

    val bobUid = signInTestUser(BOB_EMAIL)
    repo.syncSignedInUserProfile(bobUid, BOB_EMAIL, "Bob", null, isEmailVerified = true)

    val liked = withTimeout(DEFAULT_TIMEOUT_MS) { repo.togglePostLike(postId).getOrThrow() }
    assertTrue(liked)

    val commentRes =
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.addComment(postId, "Bob", "সুন্দর!").getOrThrow() }
    assertTrue(commentRes.isNotBlank())

    val comments =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeComments(postId).first { it.isNotEmpty() }
      }
    assertEquals("সুন্দর!", comments.first().text)
  }

  @Test
  fun privateChatAndMessaging_participantsOnly() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = SocialChatRepository(firestore, auth)
    repo.syncSignedInUserProfile(aliceUid, ALICE_EMAIL, "Alice", null, isEmailVerified = true)

    val bobUid = signInTestUser(BOB_EMAIL)
    repo.syncSignedInUserProfile(bobUid, BOB_EMAIL, "Bob", null, isEmailVerified = true)

    val chatId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .createOrOpenPrivateChat("Bob", PublicProfile(uid = aliceUid, displayName = "Alice"))
          .getOrThrow()
      }

    val msgId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.sendMessage(chatId, "Bob", "কেমন আছো?", listOf(aliceUid, bobUid)).getOrThrow()
      }
    assertTrue(msgId.isNotBlank())

    val messages =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeChatMessages(chatId).first { list -> list.any { it.id == msgId } }
      }
    assertTrue(messages.any { it.id == msgId })

    // Charlie (non-participant) cannot read the private chat
    signInTestUser(CHARLIE_EMAIL)
    var deniedCode: FirebaseFirestoreException.Code? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { firestore.collection("chats").document(chatId).get().await() }
    } catch (e: FirebaseFirestoreException) {
      deniedCode = e.code
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, deniedCode)
  }

  @Test
  fun regularUser_cannotAccessAdminCollectionsOrUnauthenticatedQueries() = runBlocking {
    val bobUid = signInTestUser(BOB_EMAIL)
    val repo = SocialChatRepository(firestore, auth)
    repo.syncSignedInUserProfile(bobUid, BOB_EMAIL, "Bob", null, isEmailVerified = true)

    // Regular user cannot publish broadcast
    val bcResult =
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.publishBroadcast("Fake", "Not allowed", "info") }
    assertTrue(bcResult.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      bcResult.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    // Regular user cannot grant themselves a verified blue tick
    val verifySelfResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.setUserVerified(bobUid, true) }
    assertTrue(verifySelfResult.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      verifySelfResult.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    // Regular user cannot spoof authorVerified=true on a post
    val spoofPostResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createPost("Spoofed verified post", "status", "Bob", null, authorVerified = true)
      }
    assertTrue(spoofPostResult.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      spoofPostResult.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    // Unauthenticated caller fails fast in repository AND is rejected by Firestore rules
    auth.signOut()
    var repoGuardError: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observePublicFeedPosts().first() }
    } catch (e: Exception) {
      repoGuardError = e
    }
    assertTrue(repoGuardError is IllegalStateException)

    var unauthError: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        firestore.collection("posts").whereEqualTo("visibility", "public").get().await()
      }
    } catch (e: Exception) {
      unauthError = e
    }
    val firestoreException = unauthError?.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_rule@test.com"
    const val BOB_EMAIL = "bob_rule@test.com"
    const val CHARLIE_EMAIL = "charlie_rule@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
