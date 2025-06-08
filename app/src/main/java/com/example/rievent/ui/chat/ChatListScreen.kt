package com.example.rievent.ui.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rievent.R
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
        title = stringResource(id = R.string.chat_list_title),
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
                if (chats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(id = R.string.chat_list_no_messages))
                        }
                    }
                } else {
                    items(chats, key = { it.id }) { chat ->
                        val otherParticipantId = chat.participantIds.firstOrNull { it != currentUserId }
                        val otherParticipantInfo = otherParticipantId?.let { chat.participantDetails[it] }

                        ChatListItem(
                            name = otherParticipantInfo?.name ?: stringResource(id = R.string.chat_list_unknown_user),
                            lastMessage = chat.lastMessageText ?: stringResource(id = R.string.chat_list_no_messages_in_chat),
                            imageUrl = otherParticipantInfo?.imageUrl,
                            onClick = {
                                navController.navigate("conversation/${chat.id}")
                            }
                        )
                        Divider()
                    }
                }
            }

            Button(
                onClick = {
                    navController.navigate("searchUsers")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(id = R.string.search_user_title))
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

        val placeholderPainter = rememberVectorPainter(image = Icons.Default.Person)

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),

            placeholder = placeholderPainter,
            error = placeholderPainter,
            contentDescription = stringResource(id = R.string.chat_list_profile_icon_description),
            modifier = Modifier
                .width(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(16.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(lastMessage, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}