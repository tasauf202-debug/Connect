package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedBadgeIcon
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.VerifiedBlue
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
  userId: String,
  displayName: String,
  email: String,
  photoUrl: String?,
  bio: String,
  profileVisibility: String,
  isAdmin: Boolean,
  isVerified: Boolean,
  isBanned: Boolean,
  isEmailVerified: Boolean,
  joinedAt: Timestamp?,
  isSaving: Boolean,
  onSaveProfile: (String, String, String) -> Unit,
  onSignOut: () -> Unit,
) {
  val normalizedVisibility = profileVisibility.ifBlank { "public" }
  var nameInput by remember(displayName) { mutableStateOf(displayName) }
  var bioInput by remember(bio) { mutableStateOf(bio) }
  var selectedVisibility by remember(normalizedVisibility) { mutableStateOf(normalizedVisibility) }

  val trimmedName = nameInput.trim()
  val trimmedBio = bioInput.trim()
  val hasChanges =
    trimmedName != displayName.trim() ||
      trimmedBio != bio.trim() ||
      selectedVisibility != normalizedVisibility
  val isNameError = trimmedName.isEmpty()

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item(key = "hero_card") {
        ProfileHeroCard(
          displayName = displayName,
          email = email,
          photoUrl = photoUrl,
          bio = bio,
          profileVisibility = normalizedVisibility,
          isAdmin = isAdmin,
          isVerified = isVerified,
          isBanned = isBanned,
        )
      }

      item(key = "verification_status_card") {
        VerificationStatusCard(isVerified = isVerified)
      }

      item(key = "visibility_card") {
        ProfileVisibilityCard(
          selectedVisibility = selectedVisibility,
          isSaving = isSaving,
          onSelectVisibility = { option ->
            selectedVisibility = option
            val safeName = trimmedName.ifBlank { displayName.trim() }
            if (safeName.isNotEmpty() && option != normalizedVisibility && !isSaving) {
              onSaveProfile(safeName, trimmedBio, option)
            }
          },
        )
      }

      item(key = "edit_profile_card") {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier =
            Modifier.fillMaxWidth()
              .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                RoundedCornerShape(20.dp),
              )
              .testTag("profile_edit_card"),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = stringResource(R.string.profile_edit_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }

            OutlinedTextField(
              value = nameInput,
              onValueChange = { if (it.length <= 80) nameInput = it },
              label = { Text(stringResource(R.string.profile_display_name_label)) },
              singleLine = true,
              isError = isNameError,
              supportingText = {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = if (isNameError) "ডিসপ্লে নাম খালি রাখা যাবে না" else "পোস্ট ও চ্যাটে এই নাম দেখাবে",
                  )
                  Text(text = "${nameInput.length}/80")
                }
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("profile_display_name_input"),
            )

            OutlinedTextField(
              value = bioInput,
              onValueChange = { if (it.length <= 200) bioInput = it },
              label = { Text(stringResource(R.string.profile_bio_label)) },
              minLines = 2,
              maxLines = 4,
              supportingText = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                  Text(text = "${bioInput.length}/200")
                }
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("profile_bio_input"),
            )

            Button(
              onClick = { onSaveProfile(trimmedName, trimmedBio, selectedVisibility) },
              enabled = !isNameError && hasChanges && !isSaving,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button"),
            ) {
              if (isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.Save,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.profile_save_button))
              }
            }
          }
        }
      }

      item(key = "details_card") {
        ProfileDetailsCard(
          userId = userId,
          displayName = displayName,
          email = email,
          profileVisibility = normalizedVisibility,
          isAdmin = isAdmin,
          isEmailVerified = isEmailVerified,
          joinedAt = joinedAt,
        )
      }

      item(key = "sign_out_action") {
        OutlinedButton(
          onClick = onSignOut,
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.tertiary,
            ),
          modifier =
            Modifier.fillMaxWidth().height(50.dp).testTag("profile_sign_out_button"),
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
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeroCard(
  displayName: String,
  email: String,
  photoUrl: String?,
  bio: String,
  profileVisibility: String,
  isAdmin: Boolean,
  isVerified: Boolean,
  isBanned: Boolean,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
          RoundedCornerShape(24.dp),
        )
        .testTag("profile_hero_card"),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f),
                  MaterialTheme.colorScheme.surface,
                ),
            )
          )
          .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      UserAvatar(
        name = displayName,
        photoUrl = photoUrl,
        size = 86.dp,
        isAdminBadge = isAdmin,
      )
      Spacer(modifier = Modifier.height(14.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = displayName.ifBlank { "Connecta User" },
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (isVerified) {
          VerifiedBadgeIcon(
            size = 22.dp,
            modifier = Modifier.testTag("profile_hero_verified_badge"),
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = email,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (bio.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = bio,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color =
            if (isAdmin) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.primaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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

        Surface(
          shape = RoundedCornerShape(50),
          color =
            if (isBanned) MaterialTheme.colorScheme.tertiaryContainer
            else EmeraldOnline.copy(alpha = 0.14f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = if (isBanned) Icons.Filled.Warning else Icons.Filled.CheckCircle,
              contentDescription = null,
              tint =
                if (isBanned) MaterialTheme.colorScheme.onTertiaryContainer else EmeraldOnline,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = if (isBanned) "সীমাবদ্ধ (Banned)" else "সক্রিয় অ্যাকাউন্ট",
              style = MaterialTheme.typography.labelMedium,
              color =
                if (isBanned) MaterialTheme.colorScheme.onTertiaryContainer else EmeraldOnline,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = visibilityIcon(profileVisibility),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = stringResource(visibilityShortRes(profileVisibility)),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }
    }
  }
}

private data class VisibilityOptionItem(
  val key: String,
  val titleRes: Int,
  val descRes: Int,
  val icon: ImageVector,
)

@Composable
private fun ProfileVisibilityCard(
  selectedVisibility: String,
  isSaving: Boolean,
  onSelectVisibility: (String) -> Unit,
) {
  val options =
    remember {
      listOf(
        VisibilityOptionItem(
          key = "public",
          titleRes = R.string.profile_visibility_public,
          descRes = R.string.profile_visibility_public_desc,
          icon = Icons.Filled.Public,
        ),
        VisibilityOptionItem(
          key = "friends",
          titleRes = R.string.profile_visibility_friends,
          descRes = R.string.profile_visibility_friends_desc,
          icon = Icons.Filled.Group,
        ),
        VisibilityOptionItem(
          key = "private",
          titleRes = R.string.profile_visibility_private,
          descRes = R.string.profile_visibility_private_desc,
          icon = Icons.Filled.Lock,
        ),
      )
    }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
          RoundedCornerShape(20.dp),
        )
        .testTag("profile_visibility_card"),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Shield,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = stringResource(R.string.profile_visibility_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
      Text(
        text = stringResource(R.string.profile_visibility_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      options.forEach { option ->
        val isSelected = selectedVisibility == option.key
        Surface(
          onClick = { onSelectVisibility(option.key) },
          enabled = !isSaving,
          shape = RoundedCornerShape(14.dp),
          color =
            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border =
            BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            ),
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 56.dp)
              .semantics {
                role = Role.RadioButton
                selected = isSelected
              }
              .testTag("visibility_option_${option.key}"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier =
                Modifier.size(38.dp)
                  .clip(CircleShape)
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surface
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint =
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(option.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = stringResource(option.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(
              imageVector =
                if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
              contentDescription = stringResource(option.titleRes),
              tint =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }
  }
}

private fun visibilityIcon(visibility: String): ImageVector =
  when (visibility) {
    "private" -> Icons.Filled.Lock
    "friends" -> Icons.Filled.Group
    else -> Icons.Filled.Public
  }

private fun visibilityShortRes(visibility: String): Int =
  when (visibility) {
    "private" -> R.string.profile_visibility_private_short
    "friends" -> R.string.profile_visibility_friends_short
    else -> R.string.profile_visibility_public_short
  }

private fun visibilityTitleRes(visibility: String): Int =
  when (visibility) {
    "private" -> R.string.profile_visibility_private
    "friends" -> R.string.profile_visibility_friends
    else -> R.string.profile_visibility_public
  }

@Composable
private fun VerificationStatusCard(isVerified: Boolean) {
  val accentColor =
    if (isVerified) VerifiedBlue else MaterialTheme.colorScheme.onSurfaceVariant
  val containerColor =
    if (isVerified) VerifiedBlue.copy(alpha = 0.08f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          if (isVerified) VerifiedBlue.copy(alpha = 0.4f)
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
          RoundedCornerShape(20.dp),
        )
        .testTag("profile_verification_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().background(containerColor).padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.profile_verification_title),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          shape = RoundedCornerShape(50),
          color =
            if (isVerified) VerifiedBlue.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surface,
        ) {
          Text(
            text = if (isVerified) "Verified" else "Unverified",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.size(44.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector =
              if (isVerified) Icons.Filled.Verified else Icons.Outlined.VerifiedUser,
            contentDescription = stringResource(R.string.cd_verified_badge),
            tint = accentColor,
            modifier = Modifier.size(24.dp),
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              stringResource(
                if (isVerified) R.string.profile_verified_title
                else R.string.profile_unverified_title
              ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text =
              stringResource(
                if (isVerified) R.string.profile_verified_desc
                else R.string.profile_unverified_desc
              ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ProfileDetailsCard(
  userId: String,
  displayName: String,
  email: String,
  profileVisibility: String,
  isAdmin: Boolean,
  isEmailVerified: Boolean,
  joinedAt: Timestamp?,
) {
  val joinedFormatted =
    remember(joinedAt) {
      joinedAt?.let {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it.toDate())
      } ?: "সদ্য যুক্ত"
    }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
          RoundedCornerShape(20.dp),
        )
        .testTag("profile_details_card"),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.profile_details_section),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      ProfileDetailRow(
        icon = Icons.Filled.Person,
        label = "পূর্ণ নাম",
        value = displayName,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
      ProfileDetailRow(
        icon = Icons.Filled.Email,
        label = if (isEmailVerified) "ইমেল (ভেরিফাইড)" else "ইমেল",
        value = email,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
      ProfileDetailRow(
        icon = visibilityIcon(profileVisibility),
        label = stringResource(R.string.profile_visibility_title),
        value = stringResource(visibilityTitleRes(profileVisibility)),
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
      ProfileDetailRow(
        icon = Icons.Filled.AdminPanelSettings,
        label = "অ্যাকাউন্ট রোল",
        value =
          stringResource(if (isAdmin) R.string.role_admin_badge else R.string.role_user_badge),
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
      ProfileDetailRow(
        icon = Icons.Filled.CalendarMonth,
        label = "যোগদানের তারিখ",
        value = joinedFormatted,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
      ProfileDetailRow(
        icon = Icons.Filled.Badge,
        label = "ইউজার আইডি",
        value = userId,
      )
    }
  }
}

@Composable
private fun ProfileDetailRow(
  icon: ImageVector,
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(20.dp),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
