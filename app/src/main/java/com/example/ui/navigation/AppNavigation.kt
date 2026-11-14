package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.repository.SocialChatRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AccountSheet
import com.example.ui.components.UserAvatar
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.NewsFeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.viewmodel.SocialChatViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_FEED = "feed"
const val ROUTE_CHATS = "chats"
const val ROUTE_PROFILE = "profile"
const val ROUTE_ADMIN_PANEL = "admin_panel"
const val ROUTE_CHAT_THREAD = "chat_thread/{chatId}"

private data class NavDestinationItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val isAdminTab: Boolean = false,
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      MainFeatureScaffold(
        currentUserId = user.uid,
        currentUserEmail = user.email.orEmpty(),
        currentUserName = user.displayName.orEmpty(),
        currentUserPhotoUrl = user.photoUrl?.toString(),
        isEmailVerified = user.isEmailVerified,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFeatureScaffold(
  currentUserId: String,
  currentUserEmail: String,
  currentUserName: String,
  currentUserPhotoUrl: String?,
  isEmailVerified: Boolean,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: SocialChatViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            SocialChatViewModel(
              repository = SocialChatRepository(db),
              currentUserId = currentUserId,
              currentUserEmail = currentUserEmail,
              currentUserName = currentUserName,
              currentUserPhotoUrl = currentUserPhotoUrl,
              isEmailVerified = isEmailVerified,
            )
          }
        },
    )

  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val currentAccount by viewModel.currentUserAccount.collectAsStateWithLifecycle()
  val currentPublicProfile by viewModel.currentPublicProfile.collectAsStateWithLifecycle()
  val effectiveDisplayName by viewModel.effectiveDisplayName.collectAsStateWithLifecycle()
  val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()
  val feedState by viewModel.feedState.collectAsStateWithLifecycle()
  val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
  val commentsState by viewModel.selectedPostComments.collectAsStateWithLifecycle()
  val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
  val chatMessagesState by viewModel.selectedChatMessages.collectAsStateWithLifecycle()
  val profiles by viewModel.discoverableProfiles.collectAsStateWithLifecycle()
  val verifiedUserIds by viewModel.verifiedUserIds.collectAsStateWithLifecycle()
  val activeBroadcasts by viewModel.activeBroadcasts.collectAsStateWithLifecycle()
  val moderationSettings by viewModel.moderationSettings.collectAsStateWithLifecycle()

  val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
  val adminIds by viewModel.adminIds.collectAsStateWithLifecycle()
  val adminAllPosts by viewModel.adminAllPosts.collectAsStateWithLifecycle()
  val adminBroadcasts by viewModel.adminBroadcasts.collectAsStateWithLifecycle()

  val isBanned = currentAccount?.isBanned == true
  val isCurrentUserVerified =
    currentAccount?.isVerified == true ||
      currentPublicProfile?.isVerified == true ||
      currentUserId in verifiedUserIds
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route

  val snackbarHostState = remember { SnackbarHostState() }
  var showAccountSheet by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.actionMessages.collect { msg -> snackbarHostState.showSnackbar(msg) }
  }

  val navItems =
    remember(isAdmin) {
      buildList {
        add(
          NavDestinationItem(
            route = ROUTE_FEED,
            labelRes = R.string.nav_feed,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
          )
        )
        add(
          NavDestinationItem(
            route = ROUTE_CHATS,
            labelRes = R.string.nav_chats,
            selectedIcon = Icons.Filled.ChatBubble,
            unselectedIcon = Icons.Outlined.ChatBubbleOutline,
          )
        )
        add(
          NavDestinationItem(
            route = ROUTE_PROFILE,
            labelRes = R.string.nav_profile,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
          )
        )
        if (isAdmin) {
          add(
            NavDestinationItem(
              route = ROUTE_ADMIN_PANEL,
              labelRes = R.string.nav_admin,
              selectedIcon = Icons.Filled.AdminPanelSettings,
              unselectedIcon = Icons.Outlined.AdminPanelSettings,
              isAdminTab = true,
            )
          )
        }
      }
    }

  val isTopLevelRoute = currentRoute in navItems.map { it.route }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        if (isTopLevelRoute) {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(32.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Forum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Text(
                  text =
                    when (currentRoute) {
                      ROUTE_CHATS -> stringResource(R.string.chats_title)
                      ROUTE_PROFILE -> stringResource(R.string.profile_title)
                      ROUTE_ADMIN_PANEL -> stringResource(R.string.admin_title)
                      else -> stringResource(R.string.feed_title)
                    },
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            },
            actions = {
              if (isAdmin) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.tertiaryContainer,
                  modifier = Modifier.padding(end = 8.dp),
                ) {
                  Text(
                    text = stringResource(R.string.role_admin_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
              Box(
                modifier =
                  Modifier.padding(end = 12.dp)
                    .clip(CircleShape)
                    .clickable { showAccountSheet = true }
                    .testTag("account_avatar_button"),
              ) {
                UserAvatar(
                  name = effectiveDisplayName,
                  photoUrl = currentUserPhotoUrl,
                  size = 38.dp,
                  isAdminBadge = isAdmin,
                )
              }
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
              ),
          )
        }
      },
      bottomBar = {
        if (isTopLevelRoute && !useNavigationRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              val activeColor =
                if (item.isAdminTab) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(item.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = stringResource(item.labelRes),
                  )
                },
                label = { Text(text = stringResource(item.labelRes)) },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = activeColor,
                    selectedTextColor = activeColor,
                    indicatorColor = activeColor.copy(alpha = 0.14f),
                  ),
                modifier = Modifier.testTag("nav_${item.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (isTopLevelRoute && useNavigationRail) {
          NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            Spacer(modifier = Modifier.size(12.dp))
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(item.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = stringResource(item.labelRes),
                  )
                },
                label = { Text(text = stringResource(item.labelRes)) },
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = ROUTE_FEED,
          modifier = Modifier.fillMaxSize(),
        ) {
          composable(ROUTE_FEED) {
            NewsFeedScreen(
              feedState = feedState,
              likedPostIds = likedPostIds,
              verifiedUserIds = verifiedUserIds,
              activeBroadcasts = activeBroadcasts,
              moderationSettings = moderationSettings,
              currentUserId = currentUserId,
              currentUserName = effectiveDisplayName,
              currentUserPhotoUrl = currentUserPhotoUrl,
              isAdmin = isAdmin,
              isBanned = isBanned,
              commentsState = commentsState,
              onRefreshLikes = { viewModel.refreshLikedStatusForPosts(it) },
              onCreatePost = { content, category -> viewModel.createPost(content, category) },
              onToggleLike = { viewModel.toggleLike(it) },
              onOpenComments = { viewModel.openCommentsForPost(it) },
              onAddComment = { postId, text -> viewModel.addComment(postId, text) },
              onDeletePost = { viewModel.deletePost(it) },
            )
          }

          composable(ROUTE_CHATS) {
            ChatScreen(
              chatsState = chatsState,
              discoverableProfiles = profiles,
              verifiedUserIds = verifiedUserIds,
              moderationSettings = moderationSettings,
              currentUserId = currentUserId,
              isAdmin = isAdmin,
              isBanned = isBanned,
              onOpenChat = { chatId -> navController.navigate("chat_thread/$chatId") },
              onStartPrivateChat = { partner ->
                viewModel.startPrivateChat(partner) { chatId ->
                  navController.navigate("chat_thread/$chatId")
                }
              },
              onCreateGroupChat = { title, members ->
                viewModel.createGroupChat(title, members) { chatId ->
                  navController.navigate("chat_thread/$chatId")
                }
              },
            )
          }

          composable(ROUTE_PROFILE) {
            ProfileScreen(
              userId = currentUserId,
              displayName = effectiveDisplayName,
              email = currentUserEmail,
              photoUrl = currentUserPhotoUrl,
              bio = currentPublicProfile?.bio.orEmpty(),
              profileVisibility =
                currentPublicProfile?.profileVisibility?.ifBlank { "public" } ?: "public",
              isAdmin = isAdmin,
              isVerified = isCurrentUserVerified,
              isBanned = isBanned,
              isEmailVerified = isEmailVerified,
              joinedAt = currentAccount?.createdAt,
              isSaving = isSavingProfile,
              onSaveProfile = { newName, newBio, visibility ->
                viewModel.updateProfile(newName, newBio, visibility)
              },
              onSignOut = { signOutUser(credentialManager, scope) },
            )
          }

          composable(ROUTE_ADMIN_PANEL) {
            if (isAdmin) {
              AdminPanelScreen(
                usersState = adminUsers,
                adminIds = adminIds,
                allPostsState = adminAllPosts,
                broadcastsState = adminBroadcasts,
                moderationSettings = moderationSettings,
                currentUserId = currentUserId,
                onSetUserBanned = { uid, banned -> viewModel.setUserBanned(uid, banned) },
                onSetUserAdmin = { uid, makeAdmin -> viewModel.setUserAdminRole(uid, makeAdmin) },
                onSetUserVerified = { uid, verified, name ->
                  viewModel.setUserVerified(uid, verified, name)
                },
                onVerifyUserByQuery = { query, onMatched ->
                  viewModel.verifyUserByQuery(query, onMatched)
                },
                onPublishBroadcast = { title, msg, prio ->
                  viewModel.publishBroadcast(title, msg, prio)
                },
                onToggleBroadcastActive = { id, active -> viewModel.setBroadcastActive(id, active) },
                onUpdateModeration = { viewModel.updateModerationSettings(it) },
                onSetPostVisibility = { id, vis -> viewModel.setPostVisibility(id, vis) },
                onDeletePost = { viewModel.deletePost(it) },
              )
            } else {
              LaunchedEffect(Unit) { navController.popBackStack(ROUTE_FEED, inclusive = false) }
            }
          }

          composable(
            route = ROUTE_CHAT_THREAD,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
          ) { entry ->
            val chatId = entry.arguments?.getString("chatId").orEmpty()
            DisposableEffect(chatId) {
              viewModel.openChatThread(chatId)
              onDispose { viewModel.openChatThread(null) }
            }
            val chatRoom =
              (chatsState as? UiState.Success)?.data?.firstOrNull { it.id == chatId }
            ChatThreadScreen(
              chatRoom = chatRoom,
              messagesState = chatMessagesState,
              verifiedUserIds = verifiedUserIds,
              currentUserId = currentUserId,
              isBanned = isBanned,
              onBack = { navController.popBackStack() },
              onSendMessage = { text ->
                val participants = chatRoom?.participantIds ?: listOf(currentUserId)
                viewModel.sendMessage(chatId, text, participants)
              },
            )
          }
        }
      }
    }
  }

  if (showAccountSheet) {
    AccountSheet(
      name = effectiveDisplayName,
      email = currentUserEmail,
      photoUrl = currentUserPhotoUrl,
      isAdmin = isAdmin,
      isVerified = isCurrentUserVerified,
      isBanned = isBanned,
      onDismiss = { showAccountSheet = false },
      onOpenProfile = {
        navController.navigate(ROUTE_PROFILE) {
          popUpTo(navController.graph.findStartDestination().id) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
      },
      onSignOut = { signOutUser(credentialManager, scope) },
    )
  }
}
