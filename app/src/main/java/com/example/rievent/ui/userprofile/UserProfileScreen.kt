package com.example.rievent.ui.userprofile

import ParticipantInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rievent.R
import com.example.rievent.models.User
import com.example.rievent.ui.allevents.AllEventCard
import com.example.rievent.ui.allevents.AllEventsViewModel
import com.example.rievent.ui.chat.ChatViewModel
import com.example.rievent.ui.utils.Drawer
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    viewModel: UserProfileViewModel = viewModel(),
    allEventsViewModel: AllEventsViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToSingleEvent: (eventId: String) -> Unit,
    isCurrentUserProfile: Boolean,
    chatViewModel: ChatViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadDataFor(userId)
        }
    }


    Drawer(
        title = uiState.user?.displayName ?: stringResource(id = R.string.user_profile_title),
        navController = navController,
        gesturesEnabled = true,

    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (uiState.isLoadingProfile) {
                    CircularProgressIndicator()
                } else if (uiState.user != null) {
                    UserProfileHeader(
                        user = uiState.user!!,
                        isFollowing = uiState.isFollowing,
                        isFollowActionLoading = uiState.isFollowActionLoading,
                        isCurrentUserProfile = isCurrentUserProfile,
                        onToggleFollow = { viewModel.toggleFollowUser(userId) },
                        onMessageClick = {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                val youString = context.getString(R.string.user_profile_you_label)
                                val userString = context.getString(R.string.user_profile_user_label)
                                val currentUserInfo = ParticipantInfo(name = currentUser.displayName ?: youString, imageUrl = currentUser.photoUrl?.toString())
                                val profileUserInfo = ParticipantInfo(name = uiState.user!!.displayName ?: userString, imageUrl = uiState.user!!.photoUrl)
                                val participantIds = listOf(currentUser.uid, uiState.user!!.uid!!).sorted()
                                val chatId = participantIds.joinToString(separator = "_")
                                val participantDetails = mapOf(currentUser.uid to currentUserInfo, uiState.user!!.uid!! to profileUserInfo)

                                chatViewModel.prepareForNewChat(participantDetails)
                                navController.navigate("conversation/$chatId")
                            }
                        }
                    )
                } else {
                    Text(
                        text = uiState.errorMessage ?: stringResource(id = R.string.user_profile_error_loading),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState.user != null) {
                item {
                    Text(
                        text = stringResource(id = R.string.user_profile_events_created_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (uiState.isLoadingEvents) {
                    item { CircularProgressIndicator() }
                } else if (uiState.createdEvents.isNotEmpty()) {
                    items(uiState.createdEvents, key = { it.id!! }) { event ->
                        AllEventCard(
                            event = event,
                            allEventsViewModel = allEventsViewModel,
                            onCardClick = onNavigateToSingleEvent
                        )
                    }
                } else {
                    item { Text(stringResource(id = R.string.user_profile_no_events)) }
                }
            }
            if (uiState.errorMessage != null && !uiState.isLoadingProfile && !uiState.isLoadingEvents) {
                item {
                    Text(
                        text = stringResource(id = R.string.user_profile_generic_error, uiState.errorMessage!!),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun UserProfileHeader(
    user: User,
    isFollowing: Boolean,
    isFollowActionLoading: Boolean,
    isCurrentUserProfile: Boolean,
    onToggleFollow: () -> Unit,
    onMessageClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (user.photoUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(user.photoUrl).crossfade(true).build()),
                contentDescription = stringResource(id = R.string.user_profile_picture_description),
                modifier = Modifier.size(120.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = stringResource(id = R.string.default_profile_picture_description),
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(user.displayName ?: stringResource(id = R.string.user_profile_no_name), style = MaterialTheme.typography.headlineMedium)
        user.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
        user.bio?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp)) }

        if (!isCurrentUserProfile) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onToggleFollow, enabled = !isFollowActionLoading) {
                    if (isFollowActionLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (isFollowing) stringResource(id = R.string.user_profile_unfollow_button) else stringResource(id = R.string.user_profile_follow_button))
                    }
                }
                Button(onClick = onMessageClick) {
                    Icon(Icons.Default.Email, contentDescription = stringResource(id = R.string.user_profile_message_button))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.user_profile_message_button))
                }
            }
        }
    }
}