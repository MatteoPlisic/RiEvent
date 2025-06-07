package com.example.rievent.ui.chat // Or your package

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.ui.utils.Drawer
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatViewModel = viewModel(),
) {
    val chats by viewModel.chatList.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid



    Drawer(
        title = "Messages",
        navController = navController,
        gesturesEnabled = true,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                // You can add a loading state here if you want
                // if (isLoading) { item { CircularProgressIndicator() } }

                if (chats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("You have no messages yet.")
                        }
                    }
                } else {
                    items(chats, key = { it.id }) { chat ->
                        val otherParticipantId = chat.participantIds.firstOrNull { it != currentUserId }
                        val otherParticipantInfo = otherParticipantId?.let { chat.participantDetails[it] }

                        ChatListItem(
                            name = otherParticipantInfo?.name ?: "Unknown User",
                            lastMessage = chat.lastMessageText ?: "No messages yet.",
                            imageUrl = otherParticipantInfo?.imageUrl,
                            onClick = {
                                navController.navigate("conversation/${chat.id}")
                            }
                        )
                        Divider()
                    }
                }
            }

            // Button to start a new chat
            Button(
                onClick = {
                    navController.navigate("searchUsers")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Start a New Chat")
            }
        }
    }
}

@Composable
fun ChatListItem(name: String, lastMessage: String, imageUrl: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // You can later replace this with a Coil/Glide image loader for imageUrl
        Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.width(48.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(lastMessage, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}