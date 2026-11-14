package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.VerifiedBlue
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VerifiedBadgeIcon(
  modifier: Modifier = Modifier,
  size: Dp = 16.dp,
) {
  Icon(
    imageVector = Icons.Filled.Verified,
    contentDescription = stringResource(R.string.cd_verified_badge),
    tint = VerifiedBlue,
    modifier = modifier.size(size),
  )
}

@Composable
fun UserAvatar(
  name: String,
  photoUrl: String?,
  size: Dp = 42.dp,
  isAdminBadge: Boolean = false,
) {
  Box(modifier = Modifier.size(size)) {
    if (!photoUrl.isNullOrBlank()) {
      AsyncImage(
        model = photoUrl,
        contentDescription = stringResource(R.string.cd_profile_avatar),
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxSize()
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
      )
    } else {
      val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
      Box(
        modifier =
          Modifier.fillMaxSize()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = initial,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
    if (isAdminBadge) {
      Box(
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.VerifiedUser,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onTertiary,
          modifier = Modifier.size(10.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
  name: String,
  email: String,
  photoUrl: String?,
  isAdmin: Boolean,
  isVerified: Boolean,
  isBanned: Boolean,
  onDismiss: () -> Unit,
  onOpenProfile: () -> Unit,
  onSignOut: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      UserAvatar(name = name, photoUrl = photoUrl, size = 72.dp, isAdminBadge = isAdmin)
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = name.ifBlank { "Connecta User" },
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (isVerified) {
          VerifiedBadgeIcon(size = 20.dp)
        }
      }
      Text(
        text = email,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        shape = RoundedCornerShape(50),
        color =
          if (isAdmin) MaterialTheme.colorScheme.tertiaryContainer
          else MaterialTheme.colorScheme.primaryContainer,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = if (isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Person,
            contentDescription = null,
            tint =
              if (isAdmin) MaterialTheme.colorScheme.onTertiaryContainer
              else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text =
              stringResource(
                if (isAdmin) R.string.role_admin_badge else R.string.role_user_badge
              ),
            style = MaterialTheme.typography.labelMedium,
            color =
              if (isAdmin) MaterialTheme.colorScheme.onTertiaryContainer
              else MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }

      if (isBanned) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = stringResource(R.string.feed_banned_notice),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      OutlinedButton(
        onClick = {
          onDismiss()
          onOpenProfile()
        },
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_profile_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Person,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.profile_open_profile),
          style = MaterialTheme.typography.labelLarge,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = {
          onDismiss()
          onSignOut()
        },
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
          ),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.account_sign_out),
          style = MaterialTheme.typography.labelLarge,
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

fun formatRelativeTimestamp(timestamp: Timestamp?): String {
  val ts = timestamp ?: return "এইমাত্র"
  val diffMillis = System.currentTimeMillis() - ts.toDate().time
  val minutes = diffMillis / 60_000L
  return when {
    minutes < 1 -> "এইমাত্র"
    minutes < 60 -> "${minutes} মি. আগে"
    minutes < 1440 -> "${minutes / 60} ঘ. আগে"
    else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(ts.toDate())
  }
}
