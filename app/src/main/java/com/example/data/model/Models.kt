package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserAccount(
  @DocumentId val id: String = "",
  val uid: String = "",
  val email: String = "",
  val displayName: String = "",
  val photoUrl: String? = null,
  @get:PropertyName("isBanned") @field:PropertyName("isBanned") val isBanned: Boolean = false,
  @get:PropertyName("isVerified") @field:PropertyName("isVerified") val isVerified: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PublicProfile(
  @DocumentId val id: String = "",
  val uid: String = "",
  val displayName: String = "",
  val photoUrl: String? = null,
  val bio: String? = null,
  @get:PropertyName("isDiscoverable")
  @field:PropertyName("isDiscoverable")
  val isDiscoverable: Boolean = true,
  val profileVisibility: String = "public",
  @get:PropertyName("isAdminBadge")
  @field:PropertyName("isAdminBadge")
  val isAdminBadge: Boolean = false,
  @get:PropertyName("isVerified") @field:PropertyName("isVerified") val isVerified: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FeedPost(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorPhotoUrl: String? = null,
  val authorVerified: Boolean = false,
  val content: String = "",
  val category: String = "status",
  val visibility: String = "public",
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PostComment(
  @DocumentId val id: String = "",
  val postId: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatRoom(
  @DocumentId val id: String = "",
  val title: String = "",
  val chatType: String = "private",
  val participantIds: List<String> = emptyList(),
  val participantNames: Map<String, String> = emptyMap(),
  val createdBy: String = "",
  val lastMessageText: String = "",
  val lastSenderId: String = "",
  val lastSenderName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val text: String = "",
  val participantIds: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BroadcastNotice(
  @DocumentId val id: String = "",
  val title: String = "",
  val message: String = "",
  val priority: String = "info",
  @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ModerationSettings(
  val allowNewPosts: Boolean = true,
  val allowNewChats: Boolean = true,
  val autoFilterEnabled: Boolean = false,
  val maintenanceNotice: String = "",
  val updatedBy: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
