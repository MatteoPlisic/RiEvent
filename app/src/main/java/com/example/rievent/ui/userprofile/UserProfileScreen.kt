package com.example.rievent.ui.userprofile

import Event // Your Event data class
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit // For an edit button
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rievent.models.User // Your User data class
import com.example.rievent.ui.allevents.AllEventCard // Re-use your event card if suitable
import com.example.rievent.ui.allevents.AllEventsViewModel // If AllEventCard needs it
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String, // The ID of the profile to display
    viewModel: UserProfileViewModel = viewModel(),
    allEventsViewModel: AllEventsViewModel = viewModel(), // If using AllEventCard and it needs its own VM
    onBack: () -> Unit,
    onNavigateToSingleEvent: (eventId: String) -> Unit,
    isCurrentUserProfile: Boolean // To know if this is the logged-in user's own profile
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
                    UserProfileHeader(user = userProfile!!)
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
                            allEventsViewModel = allEventsViewModel // Pass the necessary ViewModel
                        )
                        // Or a simpler card:
                        // EventItemCard(event = event, onNavigateToSingleEvent = onNavigateToSingleEvent)
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
fun UserProfileHeader(user: User) {
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
        // Add more fields as needed (e.g., join date)
        // user.createdAt?.let {
        //    val formatter = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
        //    Text("Joined: ${formatter.format(it.toDate())}", style = MaterialTheme.typography.bodySmall)
        // }
    }
}

// Optional: A simpler event card for the profile screen
@Composable
fun EventItemCard(event: Event, onNavigateToSingleEvent: (String) -> Unit) {
    Card(
        onClick = { event.id?.let { onNavigateToSingleEvent(it) } },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium)
            event.startTime?.let {
                // Simple date formatting
                Text(
                    "Date: ${java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it.toDate())}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}