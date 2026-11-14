package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BroadcastNotice
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.FeedPost
import com.example.data.model.ModerationSettings
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.UserAccount
import com.example.data.repository.SocialChatRepository
import com.example.data.util.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class SocialChatViewModel(
  private val repository: SocialChatRepository,
  val currentUserId: String,
  val currentUserEmail: String,
  val currentUserName: String,
  val currentUserPhotoUrl: String?,
  val isEmailVerified: Boolean,
) : ViewModel() {

  private val _actionMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val actionMessages: SharedFlow<String> = _actionMessages.asSharedFlow()

  private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
  val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()
  private val _likingPostIds = MutableStateFlow<Set<String>>(emptySet())

  private val _selectedPostIdForComments = MutableStateFlow<String?>(null)
  private val _selectedChatId = MutableStateFlow<String?>(null)

  init {
    viewModelScope.launch {
      repository.syncSignedInUserProfile(
        uid = currentUserId,
        email = currentUserEmail,
        displayName = currentUserName,
        photoUrl = currentUserPhotoUrl,
        isEmailVerified = isEmailVerified,
      )
    }
  }

  val isAdmin: StateFlow<Boolean> =
    repository
      .observeIsAdmin(currentUserId, currentUserEmail, isEmailVerified)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false,
      )

  private val _isSavingProfile = MutableStateFlow(false)
  val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

  val currentUserAccount: StateFlow<UserAccount?> =
    repository
      .observeCurrentUserAccount(currentUserId)
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing current user account", firestoreError ?: error)
        emit(null)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val currentPublicProfile: StateFlow<PublicProfile?> =
    repository
      .observeCurrentPublicProfile(currentUserId)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val effectiveDisplayName: StateFlow<String> =
    currentUserAccount
      .map { account ->
        account?.displayName?.takeIf { it.isNotBlank() }
          ?: currentUserName.ifBlank { currentUserEmail.substringBefore("@").ifBlank { "ব্যবহারকারী" } }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue =
          currentUserName.ifBlank { currentUserEmail.substringBefore("@").ifBlank { "ব্যবহারকারী" } },
      )

  val feedState: StateFlow<UiState<List<FeedPost>>> =
    repository
      .observePublicFeedPosts()
      .toUiState("Failed to load news feed")
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val chatsState: StateFlow<UiState<List<ChatRoom>>> =
    repository
      .observeMyChats(currentUserId)
      .toUiState("Failed to load chats")
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val discoverableProfiles: StateFlow<List<PublicProfile>> =
    repository
      .observeDiscoverableProfiles()
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing profiles", firestoreError ?: error)
        emit(emptyList())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val verifiedUserIds: StateFlow<Set<String>> =
    discoverableProfiles
      .map { profiles -> profiles.filter { it.isVerified }.map { it.uid }.toSet() }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet(),
      )

  val activeBroadcasts: StateFlow<List<BroadcastNotice>> =
    repository
      .observeActiveBroadcasts()
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing active broadcasts", firestoreError ?: error)
        emit(emptyList())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val moderationSettings: StateFlow<ModerationSettings> =
    repository
      .observeModerationSettings()
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing moderation settings", firestoreError ?: error)
        emit(ModerationSettings())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ModerationSettings(),
      )

  val selectedPostComments: StateFlow<UiState<List<PostComment>>> =
    _selectedPostIdForComments
      .flatMapLatest { postId ->
        if (postId.isNullOrBlank()) flowOf(UiState.Success(emptyList()))
        else repository.observeComments(postId).toUiState("Failed to load comments")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val selectedChatMessages: StateFlow<UiState<List<ChatMessage>>> =
    _selectedChatId
      .flatMapLatest { chatId ->
        if (chatId.isNullOrBlank()) flowOf(UiState.Success(emptyList()))
        else repository.observeChatMessages(chatId).toUiState("Failed to load messages")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  // Admin-gated streams (only attached when isAdmin == true)
  val adminUsers: StateFlow<UiState<List<UserAccount>>> =
    isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeAllUsersForAdmin().toUiState("Failed to load users")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminIds: StateFlow<Set<String>> =
    isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(emptySet()) else repository.observeAdminIdsForAdmin()
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet(),
      )

  val adminAllPosts: StateFlow<UiState<List<FeedPost>>> =
    isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeAllPostsForAdmin().toUiState("Failed to load all posts")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminBroadcasts: StateFlow<UiState<List<BroadcastNotice>>> =
    isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeAllBroadcastsForAdmin().toUiState("Failed to load broadcasts")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  fun refreshLikedStatusForPosts(posts: List<FeedPost>) {
    viewModelScope.launch {
      val updated = mutableSetOf<String>()
      for (post in posts.take(30)) {
        if (repository.isPostLikedByUser(post.id)) {
          updated.add(post.id)
        }
      }
      _likedPostIds.value = updated
    }
  }

  fun openCommentsForPost(postId: String?) {
    _selectedPostIdForComments.value = postId
  }

  fun openChatThread(chatId: String?) {
    _selectedChatId.value = chatId
  }

  fun createPost(content: String, category: String) {
    if (content.isBlank()) {
      _actionMessages.tryEmit("দয়া করে পোস্টের লেখা লিখুন।")
      return
    }
    val isAuthorVerified =
      currentUserAccount.value?.isVerified == true ||
        currentPublicProfile.value?.isVerified == true ||
        currentUserId in verifiedUserIds.value
    val activeName = effectiveDisplayName.value
    viewModelScope.launch {
      val res =
        repository.createPost(
          content = content,
          category = category,
          authorName = activeName,
          authorPhotoUrl = currentUserPhotoUrl,
          authorVerified = isAuthorVerified,
        )
      if (res.isSuccess) {
        _actionMessages.tryEmit("পোস্ট সফলভাবে প্রকাশ হয়েছে!")
      } else {
        _actionMessages.tryEmit("পোস্ট প্রকাশ করা যায়নি: ${res.exceptionOrNull()?.localizedMessage ?: "ত্রুটি"}")
      }
    }
  }

  fun toggleLike(postId: String) {
    if (postId in _likingPostIds.value) return
    _likingPostIds.value = _likingPostIds.value + postId
    viewModelScope.launch {
      try {
        val res = repository.togglePostLike(postId)
        res.onSuccess { nowLiked ->
          _likedPostIds.value =
            if (nowLiked) _likedPostIds.value + postId else _likedPostIds.value - postId
        }
        res.onFailure {
          _actionMessages.tryEmit("লাইক আপডেট করা যায়নি।")
        }
      } finally {
        _likingPostIds.value = _likingPostIds.value - postId
      }
    }
  }

  fun addComment(postId: String, text: String) {
    if (text.isBlank()) return
    val activeName = effectiveDisplayName.value
    viewModelScope.launch {
      val res = repository.addComment(postId, activeName, text)
      if (res.isFailure) {
        _actionMessages.tryEmit("কমেন্ট পাঠানো যায়নি।")
      }
    }
  }

  fun deletePost(postId: String) {
    viewModelScope.launch {
      val res = repository.deletePost(postId)
      if (res.isSuccess) _actionMessages.tryEmit("পোস্ট মুছে ফেলা হয়েছে।")
      else _actionMessages.tryEmit("পোস্ট মোছা যায়নি।")
    }
  }

  fun startPrivateChat(partner: PublicProfile, onReady: (String) -> Unit) {
    val activeName = effectiveDisplayName.value
    viewModelScope.launch {
      val res = repository.createOrOpenPrivateChat(activeName, partner)
      res.onSuccess { chatId -> onReady(chatId) }
      res.onFailure { _actionMessages.tryEmit("চ্যাট শুরু করা যায়নি।") }
    }
  }

  fun createGroupChat(title: String, members: List<PublicProfile>, onReady: (String) -> Unit) {
    if (title.isBlank()) {
      _actionMessages.tryEmit("গ্রুপের নাম দিন।")
      return
    }
    val activeName = effectiveDisplayName.value
    viewModelScope.launch {
      val res = repository.createGroupChat(title, activeName, members)
      res.onSuccess { chatId ->
        _actionMessages.tryEmit("গ্রুপ চ্যাট তৈরি হয়েছে!")
        onReady(chatId)
      }
      res.onFailure { _actionMessages.tryEmit("গ্রুপ তৈরি করা যায়নি।") }
    }
  }

  fun sendMessage(chatId: String, text: String, participantIds: List<String>) {
    if (text.isBlank()) return
    val activeName = effectiveDisplayName.value
    viewModelScope.launch {
      val res = repository.sendMessage(chatId, activeName, text, participantIds)
      if (res.isFailure) {
        _actionMessages.tryEmit("মেসেজ পাঠানো যায়নি।")
      }
    }
  }

  fun updateProfile(
    displayName: String,
    bio: String,
    profileVisibility: String = currentPublicProfile.value?.profileVisibility ?: "public",
  ) {
    val cleanName = displayName.trim()
    if (cleanName.isBlank()) {
      _actionMessages.tryEmit("দয়া করে একটি ডিসপ্লে নাম লিখুন।")
      return
    }
    if (_isSavingProfile.value) return
    viewModelScope.launch {
      _isSavingProfile.value = true
      try {
        val res = repository.updateUserProfile(cleanName, bio, profileVisibility)
        if (res.isSuccess) {
          _actionMessages.tryEmit("প্রোফাইল সফলভাবে আপডেট হয়েছে!")
        } else {
          _actionMessages.tryEmit("প্রোফাইল আপডেট করা যায়নি।")
        }
      } finally {
        _isSavingProfile.value = false
      }
    }
  }

  // --- Admin Actions ---
  fun publishBroadcast(title: String, message: String, priority: String) {
    if (title.isBlank() || message.isBlank()) {
      _actionMessages.tryEmit("শিরোনাম ও বার্তা দুটিই লিখুন।")
      return
    }
    viewModelScope.launch {
      val res = repository.publishBroadcast(title, message, priority)
      if (res.isSuccess) _actionMessages.tryEmit("ব্রডকাস্ট নোটিফিকেশন পাঠানো হয়েছে!")
      else _actionMessages.tryEmit("ব্রডকাস্ট পাঠানো যায়নি।")
    }
  }

  fun setBroadcastActive(broadcastId: String, active: Boolean) {
    viewModelScope.launch {
      val res = repository.setBroadcastActive(broadcastId, active)
      if (res.isSuccess) {
        _actionMessages.tryEmit(if (active) "ব্রডকাস্ট সক্রিয় করা হয়েছে।" else "ব্রডকাস্ট বন্ধ করা হয়েছে।")
      }
    }
  }

  fun setUserBanned(targetUid: String, banned: Boolean) {
    if (targetUid == currentUserId && banned) {
      _actionMessages.tryEmit("নিজের অ্যাকাউন্ট ব্যান করা যাবে না।")
      return
    }
    viewModelScope.launch {
      val res = repository.setUserBanned(targetUid, banned)
      if (res.isSuccess) {
        _actionMessages.tryEmit(if (banned) "ইউজারকে ব্যান করা হয়েছে।" else "ইউজারের ব্যান তুলে নেওয়া হয়েছে।")
      } else {
        _actionMessages.tryEmit("ইউজার স্ট্যাটাস পরিবর্তন করা যায়নি।")
      }
    }
  }

  fun setUserAdminRole(targetUid: String, makeAdmin: Boolean) {
    viewModelScope.launch {
      val res = repository.setUserAdminRole(targetUid, makeAdmin)
      if (res.isSuccess) {
        _actionMessages.tryEmit(if (makeAdmin) "অ্যাডমিন রোল দেওয়া হয়েছে।" else "অ্যাডমিন রোল সরানো হয়েছে।")
      } else {
        _actionMessages.tryEmit("রোল পরিবর্তন করা যায়নি।")
      }
    }
  }

  fun setUserVerified(targetUid: String, verified: Boolean, displayName: String? = null) {
    viewModelScope.launch {
      val res = repository.setUserVerified(targetUid, verified)
      if (res.isSuccess) {
        val msg =
          if (verified) {
            if (!displayName.isNullOrBlank()) {
              "\"$displayName\" অ্যাকাউন্টটিকে ভেরিফাইড (ব্লু টিক) ব্যাজ দেওয়া হয়েছে!"
            } else {
              "ইউজারকে ভেরিফাইড (ব্লু টিক) ব্যাজ দেওয়া হয়েছে!"
            }
          } else {
            "ভেরিফাইড ব্যাজ (ব্লু টিক) সরানো হয়েছে।"
          }
        _actionMessages.tryEmit(msg)
      } else {
        _actionMessages.tryEmit("ভেরিফাইড স্ট্যাটাস পরিবর্তন করা যায়নি।")
      }
    }
  }

  fun verifyUserByQuery(query: String, onMatched: () -> Unit = {}) {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) {
      _actionMessages.tryEmit("দয়া করে ইউজারের নাম বা ইমেল লিখুন।")
      return
    }
    val users = (adminUsers.value as? UiState.Success)?.data.orEmpty()
    val exactMatch =
      users.firstOrNull {
        it.email.equals(cleanQuery, ignoreCase = true) ||
          it.displayName.equals(cleanQuery, ignoreCase = true)
      }
    val matches =
      if (exactMatch != null) listOf(exactMatch)
      else {
        users.filter {
          it.email.contains(cleanQuery, ignoreCase = true) ||
            it.displayName.contains(cleanQuery, ignoreCase = true)
        }
      }
    when {
      matches.isEmpty() -> {
        _actionMessages.tryEmit("এই নাম বা ইমেলে কোনো নিবন্ধিত ইউজার পাওয়া যায়নি।")
      }
      matches.size > 1 -> {
        _actionMessages.tryEmit("একাধিক ইউজার মিলেছে (${matches.size} জন)। অনুগ্রহ করে সঠিক ইমেল লিখুন বা নিচের তালিকা থেকে বেছে নিন।")
      }
      else -> {
        val target = matches.first()
        setUserVerified(target.uid, true, target.displayName)
        onMatched()
      }
    }
  }

  fun updateModerationSettings(settings: ModerationSettings) {
    viewModelScope.launch {
      val res = repository.updateModerationSettings(settings)
      if (res.isSuccess) _actionMessages.tryEmit("মডারেশন সেটিংস সংরক্ষিত হয়েছে!")
      else _actionMessages.tryEmit("সেটিংস সংরক্ষণ করা যায়নি।")
    }
  }

  fun setPostVisibility(postId: String, visibility: String) {
    viewModelScope.launch {
      val res = repository.setPostVisibility(postId, visibility)
      if (res.isSuccess) {
        _actionMessages.tryEmit(if (visibility == "hidden") "পোস্টটি লুকানো হয়েছে।" else "পোস্টটি পুনরায় দেখানো হচ্ছে।")
      }
    }
  }

  private fun <T> Flow<T>.toUiState(defaultError: String): Flow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, defaultError, rootError)
        emit(UiState.Error(rootError.message ?: defaultError))
      }

  private companion object {
    const val TAG = "SocialChatVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
