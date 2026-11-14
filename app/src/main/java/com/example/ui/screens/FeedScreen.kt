package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.data.model.BroadcastNotice
import com.example.data.model.FeedPost
import com.example.data.model.ModerationSettings
import com.example.data.model.PostComment
import com.example.ui.viewmodel.UiState

@Composable
fun FeedScreen(
  feedState: UiState<List<FeedPost>>,
  likedPostIds: Set<String>,
  verifiedUserIds: Set<String>,
  activeBroadcasts: List<BroadcastNotice>,
  moderationSettings: ModerationSettings,
  currentUserId: String,
  currentUserName: String,
  currentUserPhotoUrl: String?,
  isAdmin: Boolean,
  isBanned: Boolean,
  commentsState: UiState<List<PostComment>>,
  onRefreshLikes: (List<FeedPost>) -> Unit,
  onCreatePost: (String, String) -> Unit,
  onToggleLike: (String) -> Unit,
  onOpenComments: (String?) -> Unit,
  onAddComment: (String, String) -> Unit,
  onDeletePost: (String) -> Unit,
) {
  NewsFeedScreen(
    feedState = feedState,
    likedPostIds = likedPostIds,
    verifiedUserIds = verifiedUserIds,
    activeBroadcasts = activeBroadcasts,
    moderationSettings = moderationSettings,
    currentUserId = currentUserId,
    currentUserName = currentUserName,
    currentUserPhotoUrl = currentUserPhotoUrl,
    isAdmin = isAdmin,
    isBanned = isBanned,
    commentsState = commentsState,
    onRefreshLikes = onRefreshLikes,
    onCreatePost = onCreatePost,
    onToggleLike = onToggleLike,
    onOpenComments = onOpenComments,
    onAddComment = onAddComment,
    onDeletePost = onDeletePost,
  )
}
