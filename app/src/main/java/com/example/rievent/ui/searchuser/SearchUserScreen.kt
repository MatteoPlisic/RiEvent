package com.example.rievent.ui.chat


import ParticipantInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rievent.models.User
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    navController: NavController,
    findUserViewModel: SearchUserViewModel = viewModel(),
    chatViewModel: ChatViewModel // This is the shared ViewModel for chat logic
) {
    // Collect the single state object from the ViewModel.
    val uiState by findUserViewModel.uiState.collectAsState()

    val currentUser = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start a New Chat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar - Reads from uiState, sends events to ViewModel
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { findUserViewModel.onSearchQueryChanged(it) },
                label = { Text("Search for a user...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            // Results List
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
            else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.searchResults, key = { it.uid!! }) { user ->
                        // Don't show the current user in their own search results
                        if (user.uid != currentUser?.uid) {
                            UserSearchResultItem(
                                user = user,
                                onClick = {
                                    if (currentUser != null) {
                                        // 1. Prepare participant info for the new chat
                                        val currentUserInfo = ParticipantInfo(name = currentUser.displayName ?: "", imageUrl = currentUser.photoUrl?.toString())
                                        val searchedUserInfo = ParticipantInfo(name = user.displayName ?: "", imageUrl = user.photoUrl)

                                        // 2. Create deterministic chatId by sorting UIDs
                                        val participantIds = listOf(currentUser.uid, user.uid).sorted()
                                        val chatId = participantIds.joinToString(separator = "_")

                                        // 3. Prepare the details map needed to create the chat document
                                        val participantDetails = mapOf(
                                            currentUser.uid to currentUserInfo,
                                            user.uid to searchedUserInfo
                                        )

                                        // 4. Pass the details to the shared ChatViewModel
                                        chatViewModel.prepareForNewChat(participantDetails)

                                        // 5. Navigate to the conversation screen
                                        navController.navigate("conversation/$chatId")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchResultItem(user: User, onClick: () -> Unit) {
    val placeholderPainter = rememberVectorPainter(image = Icons.Default.Person)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.photoUrl)
                .crossfade(true)
                .build(),
            placeholder = placeholderPainter,
            error = placeholderPainter,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Text(user.displayName ?: "Unknown User", style = MaterialTheme.typography.bodyLarge)
    }
}