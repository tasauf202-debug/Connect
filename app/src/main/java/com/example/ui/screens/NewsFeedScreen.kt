package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BroadcastNotice
import com.example.data.model.FeedPost
import com.example.data.model.ModerationSettings
import com.example.data.model.PostComment
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedBadgeIcon
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.viewmodel.UiState

@Composable
fun NewsFeedScreen(
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
  var selectedCategory by rememberSaveable { mutableStateOf("all") }
  var showCreateSheet by remember { mutableStateOf(false) }
  var activeCommentPostId by rememberSaveable { mutableStateOf<String?>(null) }

  val canPost = !isBanned && (moderationSettings.allowNewPosts || isAdmin)
  val activeCommentPost =
    remember(activeCommentPostId, feedState) {
      activeCommentPostId?.let { postId ->
        (feedState as? UiState.Success)?.data?.firstOrNull { it.id == postId }
      }
    }

  LaunchedEffect(feedState) {
    if (feedState is UiState.Success) {
      onRefreshLikes(feedState.data)
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 640.dp).testTag("news_feed_list"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (activeBroadcasts.isNotEmpty()) {
        item(key = "broadcast_banner") {
          BroadcastBannerCard(broadcast = activeBroadcasts.first())
        }
      }

      if (
        moderationSettings.maintenanceNotice.isNotBlank() ||
          isBanned ||
          !moderationSettings.allowNewPosts
      ) {
        item(key = "moderation_notice") {
          val noticeText =
            when {
              isBanned -> stringResource(R.string.feed_banned_notice)
              !moderationSettings.allowNewPosts -> stringResource(R.string.feed_posts_paused)
              else -> moderationSettings.maintenanceNotice
            }
          ModerationBanner(text = noticeText, isAlert = isBanned || !moderationSettings.allowNewPosts)
        }
      }

      item(key = "quick_compose") {
        QuickComposeCard(
          userName = currentUserName,
          userPhotoUrl = currentUserPhotoUrl,
          enabled = canPost,
          onClick = { if (canPost) showCreateSheet = true },
        )
      }

      item(key = "category_chips") {
        val categories =
          listOf(
            "all" to "সব পোস্ট",
            "status" to "স্ট্যাটাস",
            "photo" to "মিডিয়া ও গল্প",
            "announcement" to "ঘোষণা",
          )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(categories, key = { it.first }) { (key, label) ->
            FilterChip(
              selected = selectedCategory == key,
              onClick = { selectedCategory = key },
              label = { Text(label) },
              modifier = Modifier.testTag("feed_filter_$key"),
            )
          }
        }
      }

      when (feedState) {
        is UiState.Loading -> {
          item(key = "loading") {
            Box(
              modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(modifier = Modifier.testTag("news_feed_loading"))
            }
          }
        }
        is UiState.Error -> {
          item(key = "error") {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.tertiaryContainer,
              modifier = Modifier.fillMaxWidth().testTag("news_feed_error"),
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Warning,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                  text = feedState.message,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
        }
        is UiState.Success -> {
          val filtered =
            if (selectedCategory == "all") feedState.data
            else feedState.data.filter { it.category == selectedCategory }

          if (filtered.isEmpty()) {
            item(key = "empty_feed") {
              EmptyFeedState(canPost = canPost, onCreateClick = { showCreateSheet = true })
            }
          } else {
            items(filtered, key = { it.id }) { post ->
              val isAuthorVerified = post.authorVerified || post.authorId in verifiedUserIds
              NewsFeedPostCard(
                post = post,
                isAuthorVerified = isAuthorVerified,
                isLiked = post.id in likedPostIds,
                canDelete = post.authorId == currentUserId || isAdmin,
                onLikeClick = { onToggleLike(post.id) },
                onCommentClick = {
                  activeCommentPostId = post.id
                  onOpenComments(post.id)
                },
                onDeleteClick = { onDeletePost(post.id) },
              )
            }
          }
        }
      }
    }

    if (canPost) {
      FloatingActionButton(
        onClick = { showCreateSheet = true },
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 20.dp)
            .testTag("create_post_fab"),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = stringResource(R.string.feed_create_post),
        )
      }
    }
  }

  if (showCreateSheet) {
    CreatePostSheet(
      onDismiss = { showCreateSheet = false },
      onPublish = { content, category ->
        onCreatePost(content, category)
        showCreateSheet = false
      },
    )
  }

  activeCommentPost?.let { post ->
    CommentsBottomSheet(
      post = post,
      verifiedUserIds = verifiedUserIds,
      isBanned = isBanned,
      commentsState = commentsState,
      onDismiss = {
        activeCommentPostId = null
        onOpenComments(null)
      },
      onSendComment = { text -> onAddComment(post.id, text) },
    )
  }
}

@Composable
private fun BroadcastBannerCard(broadcast: BroadcastNotice) {
  val isUrgent = broadcast.priority == "urgent"
  val containerColor =
    if (isUrgent) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.primaryContainer
  val contentColor =
    if (isUrgent) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onPrimaryContainer

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = containerColor,
    modifier = Modifier.fillMaxWidth().testTag("active_broadcast_banner"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(36.dp)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Campaign,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(20.dp),
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = broadcast.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = contentColor,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = broadcast.message,
          style = MaterialTheme.typography.bodyMedium,
          color = contentColor.copy(alpha = 0.9f),
        )
      }
    }
  }
}

@Composable
private fun ModerationBanner(text: String, isAlert: Boolean) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color =
      if (isAlert) MaterialTheme.colorScheme.tertiaryContainer
      else MaterialTheme.colorScheme.secondaryContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = if (isAlert) Icons.Filled.Warning else Icons.Filled.Info,
        contentDescription = null,
        tint =
          if (isAlert) MaterialTheme.colorScheme.onTertiaryContainer
          else MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (isAlert) MaterialTheme.colorScheme.onTertiaryContainer
          else MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }
  }
}

@Composable
private fun QuickComposeCard(
  userName: String,
  userPhotoUrl: String?,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          RoundedCornerShape(20.dp),
        )
        .clickable(enabled = enabled, onClick = onClick)
        .testTag("quick_compose_card"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      UserAvatar(name = userName, photoUrl = userPhotoUrl, size = 42.dp)
      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = stringResource(R.string.feed_compose_hint),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
      }
      Icon(
        imageVector = Icons.Filled.PhotoLibrary,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun NewsFeedPostCard(
  post: FeedPost,
  isAuthorVerified: Boolean,
  isLiked: Boolean,
  canDelete: Boolean,
  onLikeClick: () -> Unit,
  onCommentClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val likeTint by
    animateColorAsState(
      targetValue =
        if (isLiked) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      label = "likeTint",
    )
  val likeScale by
    animateFloatAsState(
      targetValue = if (isLiked) 1.12f else 1.0f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "likeScale",
    )

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
          RoundedCornerShape(20.dp),
        )
        .testTag("post_card_${post.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(name = post.authorName, photoUrl = post.authorPhotoUrl, size = 42.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Text(
              text = post.authorName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            if (isAuthorVerified) {
              VerifiedBadgeIcon(
                size = 17.dp,
                modifier = Modifier.testTag("verified_badge_post_${post.id}"),
              )
            }
          }
          Text(
            text = formatRelativeTimestamp(post.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        val categoryLabel =
          when (post.category) {
            "photo" -> "মিডিয়া"
            "announcement" -> "ঘোষণা"
            else -> "স্ট্যাটাস"
          }
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ) {
          Text(
            text = categoryLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        if (canDelete) {
          IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_post_${post.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = "Delete post",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = post.content,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        Surface(
          onClick = onLikeClick,
          shape = RoundedCornerShape(12.dp),
          color =
            if (isLiked) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .semantics { role = Role.Button }
              .testTag("like_button_${post.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
              contentDescription = "Like",
              tint = likeTint,
              modifier = Modifier.size(20.dp).scale(likeScale),
            )
            Text(
              text = "${post.likesCount} লাইক",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
              color = likeTint,
            )
          }
        }

        Surface(
          onClick = onCommentClick,
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .semantics { role = Role.Button }
              .testTag("comment_button_${post.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.ChatBubbleOutline,
              contentDescription = "Comments",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "${post.commentsCount} কমেন্ট",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyFeedState(canPost: Boolean, onCreateClick: () -> Unit) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_feed_empty),
        contentDescription = stringResource(R.string.cd_empty_feed),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.feed_empty_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.feed_empty_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (canPost) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateClick, shape = RoundedCornerShape(14.dp)) {
          Text(text = stringResource(R.string.feed_create_post))
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostSheet(
  onDismiss: () -> Unit,
  onPublish: (String, String) -> Unit,
) {
  var content by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("status") }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = stringResource(R.string.feed_create_post),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("status" to "স্ট্যাটাস", "photo" to "মিডিয়া", "announcement" to "ঘোষণা").forEach {
          (key, label) ->
          FilterChip(
            selected = category == key,
            onClick = { category = key },
            label = { Text(label) },
          )
        }
      }

      OutlinedTextField(
        value = content,
        onValueChange = { if (it.length <= 1000) content = it },
        label = { Text("আপনার পোস্ট লিখুন") },
        minLines = 4,
        maxLines = 8,
        modifier = Modifier.fillMaxWidth().testTag("post_content_input"),
        shape = RoundedCornerShape(16.dp),
      )

      Button(
        onClick = { onPublish(content, category) },
        enabled = content.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_post_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text(text = stringResource(R.string.feed_publish))
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
  post: FeedPost,
  verifiedUserIds: Set<String>,
  isBanned: Boolean,
  commentsState: UiState<List<PostComment>>,
  onDismiss: () -> Unit,
  onSendComment: (String) -> Unit,
) {
  var commentText by remember { mutableStateOf("") }
  val liveCommentCount =
    (commentsState as? UiState.Success)?.data?.size?.toLong() ?: post.commentsCount

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .navigationBarsPadding()
          .imePadding()
          .testTag("comments_bottom_sheet"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "${stringResource(R.string.feed_comments_title)} • ${post.authorName}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (post.authorVerified || post.authorId in verifiedUserIds) {
            VerifiedBadgeIcon(size = 18.dp)
          }
        }
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = "$liveCommentCount কমেন্ট",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = post.content,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
      Spacer(modifier = Modifier.height(10.dp))

      when (commentsState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Text(
            text = commentsState.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 16.dp),
          )
        }
        is UiState.Success -> {
          AnimatedVisibility(visible = commentsState.data.isEmpty()) {
            Text(
              text = "এখনও কোনো কমেন্ট নেই। প্রথম কমেন্ট করুন!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 20.dp),
            )
          }
          if (commentsState.data.isNotEmpty()) {
            LazyColumn(
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 120.dp, max = 300.dp)
                  .testTag("comments_list"),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(commentsState.data, key = { it.id }) { comment ->
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                  modifier = Modifier.fillMaxWidth().testTag("comment_item_${comment.id}"),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    UserAvatar(name = comment.authorName, photoUrl = null, size = 32.dp)
                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                          Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                          )
                          if (comment.authorId in verifiedUserIds) {
                            VerifiedBadgeIcon(size = 14.dp)
                          }
                        }
                        Text(
                          text = formatRelativeTimestamp(comment.createdAt),
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isBanned) {
        Text(
          text = stringResource(R.string.feed_banned_notice),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(vertical = 8.dp),
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = commentText,
            onValueChange = { if (it.length <= 500) commentText = it },
            placeholder = { Text(stringResource(R.string.feed_comment_hint)) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("comment_input"),
            shape = RoundedCornerShape(14.dp),
          )
          IconButton(
            onClick = {
              if (commentText.isNotBlank()) {
                onSendComment(commentText)
                commentText = ""
              }
            },
            enabled = commentText.isNotBlank(),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("send_comment_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send comment",
              tint =
                if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
