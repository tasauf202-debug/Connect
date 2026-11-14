package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BroadcastNotice
import com.example.data.model.FeedPost
import com.example.data.model.ModerationSettings
import com.example.data.model.UserAccount
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedBadgeIcon
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.UiState

@Composable
fun AdminPanelScreen(
  usersState: UiState<List<UserAccount>>,
  adminIds: Set<String>,
  allPostsState: UiState<List<FeedPost>>,
  broadcastsState: UiState<List<BroadcastNotice>>,
  moderationSettings: ModerationSettings,
  currentUserId: String,
  onSetUserBanned: (String, Boolean) -> Unit,
  onSetUserAdmin: (String, Boolean) -> Unit,
  onSetUserVerified: (String, Boolean, String?) -> Unit,
  onVerifyUserByQuery: (String, () -> Unit) -> Unit,
  onPublishBroadcast: (String, String, String) -> Unit,
  onToggleBroadcastActive: (String, Boolean) -> Unit,
  onUpdateModeration: (ModerationSettings) -> Unit,
  onSetPostVisibility: (String, String) -> Unit,
  onDeletePost: (String) -> Unit,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val allUsers = (usersState as? UiState.Success)?.data.orEmpty()

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
      AdminHeaderCard(
        usersCount = allUsers.size,
        verifiedCount = allUsers.count { it.isVerified },
        postsCount = (allPostsState as? UiState.Success)?.data?.size ?: 0,
        activeBroadcastsCount =
          (broadcastsState as? UiState.Success)?.data?.count { it.isActive } ?: 0,
      )

      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.tertiary,
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text(stringResource(R.string.admin_tab_users)) },
          modifier = Modifier.testTag("admin_tab_users"),
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text(stringResource(R.string.admin_tab_broadcast)) },
          modifier = Modifier.testTag("admin_tab_broadcast"),
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text(stringResource(R.string.admin_tab_moderation)) },
          modifier = Modifier.testTag("admin_tab_moderation"),
        )
      }

      when (selectedTab) {
        0 ->
          UsersManagementTab(
            usersState = usersState,
            adminIds = adminIds,
            currentUserId = currentUserId,
            onSetUserBanned = onSetUserBanned,
            onSetUserAdmin = onSetUserAdmin,
            onSetUserVerified = onSetUserVerified,
            onVerifyUserByQuery = onVerifyUserByQuery,
          )
        1 ->
          BroadcastTab(
            broadcastsState = broadcastsState,
            onPublishBroadcast = onPublishBroadcast,
            onToggleBroadcastActive = onToggleBroadcastActive,
          )
        else ->
          ModerationTab(
            settings = moderationSettings,
            postsState = allPostsState,
            onUpdateSettings = onUpdateModeration,
            onSetPostVisibility = onSetPostVisibility,
            onDeletePost = onDeletePost,
          )
      }
    }
  }
}

@Composable
private fun AdminHeaderCard(
  usersCount: Int,
  verifiedCount: Int,
  postsCount: Int,
  activeBroadcastsCount: Int,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.tertiaryContainer,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(44.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.tertiary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiary,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = stringResource(R.string.admin_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
          Text(
            text = stringResource(R.string.admin_verify_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminStatChip(label = "ইউজার", value = "$usersCount", modifier = Modifier.weight(1f))
        AdminStatChip(label = "ব্লু টিক", value = "$verifiedCount", modifier = Modifier.weight(1f))
        AdminStatChip(label = "পোস্ট", value = "$postsCount", modifier = Modifier.weight(1f))
        AdminStatChip(
          label = "নোটিশ",
          value = "$activeBroadcastsCount",
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun AdminStatChip(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun UsersManagementTab(
  usersState: UiState<List<UserAccount>>,
  adminIds: Set<String>,
  currentUserId: String,
  onSetUserBanned: (String, Boolean) -> Unit,
  onSetUserAdmin: (String, Boolean) -> Unit,
  onSetUserVerified: (String, Boolean, String?) -> Unit,
  onVerifyUserByQuery: (String, () -> Unit) -> Unit,
) {
  var targetUserQuery by rememberSaveable { mutableStateOf("") }
  var showOnlyVerified by rememberSaveable { mutableStateOf(false) }

  when (usersState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is UiState.Error -> {
      Text(
        text = usersState.message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp),
      )
    }
    is UiState.Success -> {
      val verifiedCount = usersState.data.count { it.isVerified }
      val filteredUsers =
        usersState.data.filter { user ->
          val matchesVerifiedFilter = !showOnlyVerified || user.isVerified
          val matchesQuery =
            targetUserQuery.isBlank() ||
              user.displayName.contains(targetUserQuery.trim(), ignoreCase = true) ||
              user.email.contains(targetUserQuery.trim(), ignoreCase = true)
          matchesVerifiedFilter && matchesQuery
        }

      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item(key = "blue_tick_grant_card") {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier =
              Modifier.fillMaxWidth()
                .border(
                  1.dp,
                  VerifiedBlue.copy(alpha = 0.35f),
                  RoundedCornerShape(18.dp),
                ),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                VerifiedBadgeIcon(size = 20.dp)
                Text(
                  text = stringResource(R.string.admin_verify_card_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
              OutlinedTextField(
                value = targetUserQuery,
                onValueChange = { if (it.length <= 120) targetUserQuery = it },
                placeholder = { Text(stringResource(R.string.admin_verify_input_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("admin_verify_input"),
              )
              Button(
                onClick = { onVerifyUserByQuery(targetUserQuery) { targetUserQuery = "" } },
                enabled = targetUserQuery.isNotBlank(),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = VerifiedBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_verify_submit_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Verified,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.admin_verify_button))
              }
            }
          }
        }

        item(key = "user_filter_chips") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = !showOnlyVerified,
              onClick = { showOnlyVerified = false },
              label = { Text("${stringResource(R.string.admin_filter_all_users)} (${usersState.data.size})") },
              modifier = Modifier.testTag("admin_filter_all_users"),
            )
            FilterChip(
              selected = showOnlyVerified,
              onClick = { showOnlyVerified = true },
              leadingIcon = { VerifiedBadgeIcon(size = 16.dp) },
              label = { Text("${stringResource(R.string.admin_filter_verified_users)} ($verifiedCount)") },
              modifier = Modifier.testTag("admin_filter_verified_users"),
            )
          }
        }

        if (filteredUsers.isEmpty()) {
          item(key = "empty_users_filtered") {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text =
                  if (showOnlyVerified) "এখনও কোনো ইউজারকে ভেরিফাইড ব্লু টিক দেওয়া হয়নি।"
                  else "কোনো ইউজার খুঁজে পাওয়া যায়নি।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        } else {
          items(filteredUsers, key = { it.uid }) { user ->
            val isTargetAdmin =
              user.uid in adminIds ||
                user.email.lowercase() in setOf("tasauf202@gmail.com", "admin@socialchat.com")
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier =
                Modifier.fillMaxWidth()
                  .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp),
                  ),
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  UserAvatar(
                    name = user.displayName,
                    photoUrl = user.photoUrl,
                    size = 42.dp,
                    isAdminBadge = isTargetAdmin,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      if (user.isVerified) {
                        VerifiedBadgeIcon(size = 17.dp)
                      }
                    }
                    Text(
                      text = user.email,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  if (user.isBanned) {
                    Surface(
                      shape = RoundedCornerShape(50),
                      color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                      Text(
                        text = "ব্যানড",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                  onClick = { onSetUserVerified(user.uid, !user.isVerified, user.displayName) },
                  modifier = Modifier.fillMaxWidth().testTag("toggle_verify_${user.uid}"),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint =
                      if (user.isVerified) MaterialTheme.colorScheme.onSurfaceVariant
                      else VerifiedBlue,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(if (user.isVerified) "ব্লু টিক সরান" else "ব্লু টিক দিন")
                }

                if (user.uid != currentUserId) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = { onSetUserBanned(user.uid, !user.isBanned) },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(12.dp),
                    ) {
                      Icon(
                        imageVector =
                          if (user.isBanned) Icons.Filled.CheckCircle else Icons.Filled.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(if (user.isBanned) "আনব্যান" else "ব্যান করুন")
                    }
                    OutlinedButton(
                      onClick = { onSetUserAdmin(user.uid, !isTargetAdmin) },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(12.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(if (isTargetAdmin) "অ্যাডমিন সরান" else "অ্যাডমিন বানান")
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BroadcastTab(
  broadcastsState: UiState<List<BroadcastNotice>>,
  onPublishBroadcast: (String, String, String) -> Unit,
  onToggleBroadcastActive: (String, Boolean) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }
  var priority by remember { mutableStateOf("important") }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "broadcast_form") {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "📢 নতুন ব্রডকাস্ট নোটিফিকেশন",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          OutlinedTextField(
            value = title,
            onValueChange = { if (it.length <= 120) title = it },
            label = { Text("নোটিশের শিরোনাম") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("broadcast_title_input"),
          )
          OutlinedTextField(
            value = message,
            onValueChange = { if (it.length <= 500) message = it },
            label = { Text("বার্তা লিখুন") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("broadcast_message_input"),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("info" to "সাধারণ", "important" to "গুরুত্বপূর্ণ", "urgent" to "জরুরি").forEach {
              (key, label) ->
              FilterChip(
                selected = priority == key,
                onClick = { priority = key },
                label = { Text(label) },
              )
            }
          }
          Button(
            onClick = {
              onPublishBroadcast(title, message, priority)
              title = ""
              message = ""
            },
            enabled = title.isNotBlank() && message.isNotBlank(),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
              ),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_broadcast_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(imageVector = Icons.Filled.Campaign, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("সবার কাছে ব্রডকাস্ট পাঠান")
          }
        }
      }
    }

    when (broadcastsState) {
      is UiState.Loading -> {
        item { CircularProgressIndicator() }
      }
      is UiState.Error -> {
        item { Text(broadcastsState.message, color = MaterialTheme.colorScheme.error) }
      }
      is UiState.Success -> {
        items(broadcastsState.data, key = { it.id }) { item ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier =
              Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = item.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = formatRelativeTimestamp(item.createdAt),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = item.isActive,
                onCheckedChange = { onToggleBroadcastActive(item.id, it) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ModerationTab(
  settings: ModerationSettings,
  postsState: UiState<List<FeedPost>>,
  onUpdateSettings: (ModerationSettings) -> Unit,
  onSetPostVisibility: (String, String) -> Unit,
  onDeletePost: (String) -> Unit,
) {
  var noticeInput by remember(settings.maintenanceNotice) {
    mutableStateOf(settings.maintenanceNotice)
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "mod_toggles") {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "⚙️ অ্যাপ মডারেশন সেটিংস",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          SettingToggleRow(
            title = "নতুন ফিড পোস্ট চালু",
            subtitle = "বন্ধ করলে সাধারণ ইউজাররা নতুন পোস্ট দিতে পারবেন না",
            checked = settings.allowNewPosts,
            onCheckedChange = { onUpdateSettings(settings.copy(allowNewPosts = it)) },
          )
          SettingToggleRow(
            title = "নতুন চ্যাট তৈরি চালু",
            subtitle = "বন্ধ করলে নতুন প্রাইভেট বা গ্রুপ চ্যাট খোলা যাবে না",
            checked = settings.allowNewChats,
            onCheckedChange = { onUpdateSettings(settings.copy(allowNewChats = it)) },
          )
          SettingToggleRow(
            title = "অটো কনটেন্ট ফিল্টার",
            subtitle = "সন্দেহজনক বার্তা চিহ্নিত করার মোড সক্রিয় রাখুন",
            checked = settings.autoFilterEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(autoFilterEnabled = it)) },
          )
          OutlinedTextField(
            value = noticeInput,
            onValueChange = { if (it.length <= 300) noticeInput = it },
            label = { Text("রক্ষণাবেক্ষণ / সতর্কতা নোটিশ (ঐচ্ছিক)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          )
          Button(
            onClick = { onUpdateSettings(settings.copy(maintenanceNotice = noticeInput)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp),
          ) {
            Text("নোটিশ সংরক্ষণ করুন")
          }
        }
      }
    }

    item(key = "posts_mod_header") {
      Text(
        text = "পোস্ট মডারেশন",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
      )
    }

    if (postsState is UiState.Success) {
      items(postsState.data, key = { it.id }) { post ->
        val isHidden = post.visibility == "hidden"
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier =
            Modifier.fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${post.authorName} • ${if (isHidden) "লুকানো" else "দৃশ্যমান"}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = post.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = {
                onSetPostVisibility(post.id, if (isHidden) "public" else "hidden")
              },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = if (isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = "Toggle visibility",
              )
            }
            IconButton(
              onClick = { onDeletePost(post.id) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Delete post",
                tint = MaterialTheme.colorScheme.tertiary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
