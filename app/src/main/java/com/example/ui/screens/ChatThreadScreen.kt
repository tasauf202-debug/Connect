package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.ui.components.VerifiedBadgeIcon
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
  chatRoom: ChatRoom?,
  messagesState: UiState<List<ChatMessage>>,
  verifiedUserIds: Set<String>,
  currentUserId: String,
  isBanned: Boolean,
  onBack: () -> Unit,
  onSendMessage: (String) -> Unit,
) {
  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val isPartnerVerified =
    chatRoom?.chatType == "private" &&
      chatRoom.participantIds.any { it != currentUserId && it in verifiedUserIds }

  val title =
    chatRoom?.let { resolveChatDisplayTitle(it, currentUserId) } ?: "চ্যাট"

  LaunchedEffect(messagesState) {
    if (messagesState is UiState.Success && messagesState.data.isNotEmpty()) {
      listState.animateScrollToItem(messagesState.data.lastIndex)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              if (isPartnerVerified) {
                VerifiedBadgeIcon(size = 16.dp)
              }
            }
            if (chatRoom != null) {
              Text(
                text =
                  if (chatRoom.chatType == "group") "${chatRoom.participantIds.size} জন সদস্য"
                  else "প্রাইভেট চ্যাট",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = messageText,
            onValueChange = { if (it.length <= 1000) messageText = it },
            placeholder = { Text(stringResource(R.string.chats_message_hint)) },
            enabled = !isBanned,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f).testTag("chat_message_input"),
          )
          IconButton(
            onClick = {
              if (messageText.isNotBlank() && !isBanned) {
                onSendMessage(messageText)
                messageText = ""
              }
            },
            enabled = messageText.isNotBlank() && !isBanned,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(
                  if (messageText.isNotBlank() && !isBanned) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant
                )
                .testTag("send_message_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint =
                if (messageText.isNotBlank() && !isBanned) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      when (messagesState) {
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Error -> {
          Text(
            text = messagesState.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp),
          )
        }
        is UiState.Success -> {
          if (messagesState.data.isEmpty()) {
            Text(
              text = "এখনও কোনো মেসেজ নেই। প্রথম মেসেজ পাঠান!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(messagesState.data, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                MessageBubble(
                  message = msg,
                  isMine = isMine,
                  showSenderName = chatRoom?.chatType == "group" && !isMine,
                  isSenderVerified = msg.senderId in verifiedUserIds,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MessageBubble(
  message: ChatMessage,
  isMine: Boolean,
  showSenderName: Boolean,
  isSenderVerified: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape =
        RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 18.dp,
          bottomStart = if (isMine) 18.dp else 4.dp,
          bottomEnd = if (isMine) 4.dp else 18.dp,
        ),
      color =
        if (isMine) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.widthIn(max = 300.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (showSenderName) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = message.senderName,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            if (isSenderVerified) {
              VerifiedBadgeIcon(size = 13.dp)
            }
          }
          Spacer(modifier = Modifier.padding(top = 2.dp))
        }
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyLarge,
          color =
            if (isMine) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = formatRelativeTimestamp(message.createdAt),
          style = MaterialTheme.typography.labelSmall,
          color =
            if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
        )
      }
    }
  }
}
