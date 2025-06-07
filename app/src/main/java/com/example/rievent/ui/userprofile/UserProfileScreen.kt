package com.example.rievent.ui.userprofile


import ParticipantInfo
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rievent.models.User
import com.example.rievent.ui.allevents.AllEventCard
import com.example.rievent.ui.allevents.AllEventsViewModel
import com.example.rievent.ui.chat.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String, // The ID of the profile to display
    viewModel: UserProfileViewModel = viewModel(),
    allEventsViewModel: AllEventsViewModel = viewModel(), // If using AllEventCard and it needs its own VM
    onBack: () -> Unit,
    onNavigateToSingleEvent: (eventId: String) -> Unit,
    isCurrentUserProfile: Boolean, // To know if this is the logged-in user's own profile
    chatViewModel: ChatViewModel,
    navController: NavController
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val createdEvents by viewModel.createdEvents.collectAsState()
    val isLoadingProfile by viewModel.isLoadingProfile.collectAsState()
    val isLoadingEvents by viewModel.isLoadingEvents.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadUserProfile(userId)
            viewModel.loadCreatedEvents(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userProfile?.displayName ?: "User Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isCurrentUserProfile) {
                        IconButton(onClick = { /* TODO: Navigate to Edit Profile Screen */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                        }
                    }
                }
            )
        }
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
                if (isLoadingProfile && userProfile == null) {
                    CircularProgressIndicator()
                } else if (userProfile != null) {
                    UserProfileHeader(
                        user = userProfile!!, viewModel,
                        isCurrentUserProfile = isCurrentUserProfile,
                        chatViewModel = chatViewModel,
                        navController = navController
                    )
                } else if (error != null && userProfile == null) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("User profile not found.")
                }
            }

            if (userProfile != null) { // Only show events section if profile loaded
                item {
                    Text(
                        "Events Created",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isLoadingEvents && createdEvents.isEmpty()) {
                    item { CircularProgressIndicator() }
                } else if (createdEvents.isNotEmpty()) {
                    items(createdEvents, key = { event -> event.id ?: event.hashCode() }) { event ->
                        // You can reuse your AllEventCard or create a simpler one
                        AllEventCard(
                            event = event,
                            allEventsViewModel = allEventsViewModel ,
                            onCardClick = { eventId -> onNavigateToSingleEvent(eventId)},// Pass the necessary ViewModel
                        )
                        // Or a simpler card:
                         //EventItemCard(event = event, onNavigateToSingleEvent = onNavigateToSingleEvent)
                    }
                } else if (!isLoadingEvents && createdEvents.isEmpty()){
                    item { Text("This user hasn't created any events yet.") }
                }

                error?.let {
                    if (!isLoadingProfile && !isLoadingEvents) { // Show general error if not specific loading
                        item { Text("An error occurred: $it", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) } // Bottom padding
        }
    }
}

@Composable
fun UserProfileHeader(
    user: User,
    viewModel: UserProfileViewModel,
    isCurrentUserProfile: Boolean,
    chatViewModel: ChatViewModel,
    navController: NavController
) {
    val isFollowing by viewModel.isFollowing.collectAsState()
    val isFollowActionLoading by viewModel.isFollowActionLoading.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (user.photoUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(data = user.photoUrl)
                        .apply {
                            crossfade(true)
                            // placeholder(R.drawable.placeholder_avatar)
                            // error(R.drawable.error_avatar)
                        }.build()
                ),
                contentDescription = "User Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Default Profile Picture",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = user.displayName ?: "No Name Provided",
            style = MaterialTheme.typography.headlineMedium
        )
        user.email?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        user.bio?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (!isCurrentUserProfile) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.toggleFollowUser(user.uid) },
                enabled = !isFollowActionLoading // Disable button during the action
            ) {
                if (isFollowActionLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isFollowing) "Unfollow" else "Follow")
                }
            }
        }
        if (!isCurrentUserProfile && currentUser != null) {
            // This can be a single button or in a Row with the Follow button
            Button(
                onClick = {
                    // 1. Prepare participant info (Your code was already correct here)
                    val currentUserInfo = ParticipantInfo(
                        name = currentUser.displayName ?: "You",
                        imageUrl = currentUser.photoUrl?.toString()
                    )
                    val profileUserInfo = ParticipantInfo(
                        name = user.displayName ?: "User",
                        imageUrl = user.photoUrl
                    )

                    // --- START OF NEW LOGIC ---

                    // 2. Create the deterministic chatId by sorting the UIDs
                    val participantIds = listOf(currentUser.uid, user.uid).sorted()
                    val chatId = participantIds.joinToString(separator = "_")

                    // 3. Prepare the details map for the ViewModel
                    val participantDetails = mapOf(
                        currentUser.uid to currentUserInfo,
                        user.uid to profileUserInfo
                    )

                    // 4. Call the prepare function in the SHARED chatViewModel
                    //    (This chatViewModel must be passed into UserProfileHeader)
                    chatViewModel.prepareForNewChat(participantDetails)

                    // 5. Navigate to the conversation screen with the generated chatId
                    try {
                        navController.navigate("conversation/$chatId")
                    }
                    catch (Exception: Exception){
                        Log.e("UserProfileScreen", "Error navigating to conversation: $Exception")
                    }
                    // --- END OF NEW LOGIC ---
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = "Message")
                Spacer(Modifier.width(8.dp))
                Text("Message")
            }
        }
    }
}
