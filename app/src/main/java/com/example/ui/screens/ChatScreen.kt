package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatRoom
import com.example.data.model.ModerationSettings
import com.example.data.model.PublicProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedBadgeIcon
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.viewmodel.UiState

@Composable
fun ChatScreen(
  chatsState: UiState<List<ChatRoom>>,
  discoverableProfiles: List<PublicProfile>,
  verifiedUserIds: Set<String>,
  moderationSettings: ModerationSettings,
  currentUserId: String,
  isAdmin: Boolean,
  isBanned: Boolean,
  onOpenChat: (String) -> Unit,
  onStartPrivateChat: (PublicProfile) -> Unit,
  onCreateGroupChat: (String, List<PublicProfile>) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var showNewChatSheet by remember { mutableStateOf(false) }

  val canStartChat = !isBanned && (moderationSettings.allowNewChats || isAdmin)

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(stringResource(R.string.chats_search_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("chat_search_input"),
      )

      if (!moderationSettings.allowNewChats && !isAdmin) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
          Text(
            text = stringResource(R.string.chats_paused),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      when (chatsState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(text = chatsState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          val filtered =
            chatsState.data.filter { room ->
              val resolvedName = resolveChatDisplayTitle(room, currentUserId)
              searchQuery.isBlank() ||
                resolvedName.contains(searchQuery, ignoreCase = true) ||
                room.lastMessageText.contains(searchQuery, ignoreCase = true)
            }

          if (filtered.isEmpty()) {
            Column(
              modifier = Modifier.fillMaxSize().padding(32.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = stringResource(R.string.chats_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = stringResource(R.string.chats_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (canStartChat) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showNewChatSheet = true }, shape = RoundedCornerShape(14.dp)) {
                  Text(stringResource(R.string.chats_new_chat))
                }
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(filtered, key = { it.id }) { room ->
                val isPartnerVerified =
                  room.chatType == "private" &&
                    room.participantIds.any { it != currentUserId && it in verifiedUserIds }
                ChatRoomRow(
                  room = room,
                  currentUserId = currentUserId,
                  isPartnerVerified = isPartnerVerified,
                  onClick = { onOpenChat(room.id) },
                )
              }
            }
          }
        }
      }
    }

    if (canStartChat) {
      FloatingActionButton(
        onClick = { showNewChatSheet = true },
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 20.dp)
            .testTag("new_chat_fab"),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(
          imageVector = Icons.Filled.AddComment,
          contentDescription = stringResource(R.string.chats_new_chat),
        )
      }
    }
  }

  if (showNewChatSheet) {
    NewChatSheet(
      profiles = discoverableProfiles.filter { it.uid != currentUserId },
      onDismiss = { showNewChatSheet = false },
      onSelectPrivate = { profile ->
        showNewChatSheet = false
        onStartPrivateChat(profile)
      },
      onCreateGroup = { title, members ->
        showNewChatSheet = false
        onCreateGroupChat(title, members)
      },
    )
  }
}

@Composable
private fun ChatRoomRow(
  room: ChatRoom,
  currentUserId: String,
  isPartnerVerified: Boolean,
  onClick: () -> Unit,
) {
  val displayTitle = resolveChatDisplayTitle(room, currentUserId)
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
          RoundedCornerShape(18.dp),
        )
        .clickable(onClick = onClick)
        .testTag("chat_item_${room.id}"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (room.chatType == "group") {
        Box(
          modifier =
            Modifier.size(46.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Group,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      } else {
        UserAvatar(name = displayTitle, photoUrl = null, size = 46.dp)
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = displayTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (isPartnerVerified) {
              VerifiedBadgeIcon(size = 15.dp)
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = formatRelativeTimestamp(room.updatedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = room.lastMessageText.ifBlank { "কথোপকথন শুরু করুন..." },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatSheet(
  profiles: List<PublicProfile>,
  onDismiss: () -> Unit,
  onSelectPrivate: (PublicProfile) -> Unit,
  onCreateGroup: (String, List<PublicProfile>) -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var groupTitle by remember { mutableStateOf("") }
  val selectedMembers = remember { mutableStateListOf<PublicProfile>() }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .navigationBarsPadding(),
    ) {
      Text(
        text = stringResource(R.string.chats_new_chat),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))

      TabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text(stringResource(R.string.chats_private_tab)) },
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text(stringResource(R.string.chats_group_tab)) },
        )
      }
      Spacer(modifier = Modifier.height(12.dp))

      if (selectedTab == 1) {
        OutlinedTextField(
          value = groupTitle,
          onValueChange = { if (it.length <= 80) groupTitle = it },
          label = { Text("গ্রুপের নাম") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        )
      }

      if (profiles.isEmpty()) {
        Text(
          text = "এখনও অন্য কোনো সদস্য পাওয়া যায়নি। অন্য অ্যাকাউন্ট থেকে লগইন করলে এখানে তালিকা দেখা যাবে।",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 24.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().height(250.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(profiles, key = { it.uid }) { profile ->
            val isSelected = selectedMembers.any { it.uid == profile.uid }
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              modifier =
                Modifier.fillMaxWidth().clickable {
                  if (selectedTab == 0) {
                    onSelectPrivate(profile)
                  } else {
                    if (isSelected) selectedMembers.removeAll { it.uid == profile.uid }
                    else selectedMembers.add(profile)
                  }
                },
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                UserAvatar(
                  name = profile.displayName,
                  photoUrl = profile.photoUrl,
                  size = 40.dp,
                  isAdminBadge = profile.isAdminBadge,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = profile.displayName,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold,
                    )
                    if (profile.isVerified) {
                      VerifiedBadgeIcon(size = 15.dp)
                    }
                  }
                  Text(
                    text = profile.bio ?: "Connecta সদস্য",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (selectedTab == 1) {
                  Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                      if (checked) {
                        if (!isSelected) selectedMembers.add(profile)
                      } else {
                        selectedMembers.removeAll { it.uid == profile.uid }
                      }
                    },
                  )
                }
              }
            }
          }
        }
      }

      if (selectedTab == 1) {
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = { onCreateGroup(groupTitle, selectedMembers.toList()) },
          enabled = groupTitle.isNotBlank(),
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text("গ্রুপ চ্যাট তৈরি করুন (${selectedMembers.size})")
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

fun resolveChatDisplayTitle(room: ChatRoom, currentUserId: String): String {
  if (room.chatType == "group") return room.title
  val otherEntry = room.participantNames.entries.firstOrNull { it.key != currentUserId }
  return otherEntry?.value?.takeIf { it.isNotBlank() } ?: room.title
}
