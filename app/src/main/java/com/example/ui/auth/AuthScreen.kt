package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isCheckingAutoSignIn by remember { mutableStateOf(true) }
  var isSigningIn by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingAutoSignIn = false
        onAuthSuccess()
      },
      onUnauthenticated = { isCheckingAutoSignIn = false },
      scope = scope,
    )
  }

  val gradient =
    Brush.verticalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
          MaterialTheme.colorScheme.background,
          MaterialTheme.colorScheme.background,
        )
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(gradient)
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 540.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Card(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_social_hero),
          contentDescription = stringResource(R.string.cd_hero_banner),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(44.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .border(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
              RoundedCornerShape(20.dp),
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          FeatureHighlightRow(
            icon = Icons.Filled.Home,
            title = stringResource(R.string.feature_feed_title),
            subtitle = stringResource(R.string.feature_feed_desc),
          )
          FeatureHighlightRow(
            icon = Icons.Filled.Forum,
            title = stringResource(R.string.feature_chat_title),
            subtitle = stringResource(R.string.feature_chat_desc),
          )
          FeatureHighlightRow(
            icon = Icons.Filled.AdminPanelSettings,
            title = stringResource(R.string.feature_role_title),
            subtitle = stringResource(R.string.feature_role_desc),
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      AnimatedVisibility(visible = errorMessage != null) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = errorMessage.orEmpty(),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
          )
        }
      }

      Button(
        onClick = {
          errorMessage = null
          isSigningIn = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isSigningIn = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isSigningIn = false
              errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = { isSigningIn = false },
          )
        },
        enabled = !isSigningIn && !isCheckingAutoSignIn,
        modifier =
          Modifier.fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      ) {
        if (isSigningIn || isCheckingAutoSignIn) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.auth_checking),
            style = MaterialTheme.typography.labelLarge,
          )
        } else {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Login,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.sign_in_with_google),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.auth_security_note),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier =
        Modifier.size(38.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
