package com.example.rievent.ui.singleevent


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

import com.example.rievent.models.EventComment
import com.example.rievent.models.EventRSPV
import coil.request.ImageRequest

// Import data classes if they are in a different package
// import com.example.rievent.models.EventRSPV
// import com.example.rievent.models.EventRating
// import com.example.rievent.models.EventComment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleEventScreen(
    eventId: String,
    viewModel: SingleEventViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    // Add other navigation callbacks if needed
) {
    val event by viewModel.event.collectAsState()
    val rsvp by viewModel.eventRsvp.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val ratingsSize by viewModel.ratingsSize.collectAsState()
    val averageRating by viewModel.averageRating.collectAsState()
    val userRating by viewModel.userRating.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    LaunchedEffect(eventId) {
        if (eventId.isNotBlank()) {
            viewModel.loadEventData(eventId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event?.name ?: "Event Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && event == null) { // Show loading only if event data is not yet available
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (event == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(error ?: "Event not found or error loading.", color = MaterialTheme.colorScheme.error)
            }
        } else {
            event?.let { currentEvent ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Event Image (if available)
                    currentEvent.imageUrl?.let { imageUrl ->
                        item {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(data = imageUrl)
                                        .apply {
                                            crossfade(true)
                                            // placeholder(R.drawable.placeholder_image) // Add placeholder
                                            // error(R.drawable.error_image) // Add error image
                                        }.build()
                                ),
                                contentDescription = "Event Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Event Title and Basic Info
                    item {
                        Text(currentEvent.name, style = MaterialTheme.typography.headlineSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("By: ${currentEvent.ownerName}", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.width(8.dp)) // Add some space between text and button
                        Button(
                            onClick = {
                                onNavigateToUserProfile(currentEvent.ownerId) // Example navigation
                            }, modifier = Modifier.height(30.dp) ){

                            Icon(
                                imageVector = Icons.Filled.Person, // Or Icons.Filled.Info, etc.
                                contentDescription = "View Owner Profile",
                                modifier = Modifier.size(ButtonDefaults.IconSize) // Default icon size for buttons
                            )
                        }}
                        currentEvent.startTime?.let {
                            val formatter = remember { SimpleDateFormat("EEE, dd MMM yyyy, hh:mm a", Locale.getDefault()).apply { timeZone = TimeZone.getDefault()} }
                            Text("Starts: ${formatter.format(it.toDate())}", style = MaterialTheme.typography.bodyMedium)
                        }
                        currentEvent.endTime?.let {
                            val formatter = remember { SimpleDateFormat("EEE, dd MMM yyyy, hh:mm a", Locale.getDefault()).apply { timeZone = TimeZone.getDefault()} }
                            Text("Ends: ${formatter.format(it.toDate())}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Category: ${currentEvent.category}", style = MaterialTheme.typography.bodyMedium)
                        Text(currentEvent.description, style = MaterialTheme.typography.bodyLarge)
                        Text("Address: ${currentEvent.address}", style = MaterialTheme.typography.bodyMedium)
                    }

                    // RSVP Section
                    item {
                        RsvpSection(
                            rsvpData = rsvp,
                            currentUserId = currentUid,
                            onRsvpChanged = { newStatus ->
                                viewModel.updateRsvp(currentEvent.id!!, newStatus)
                            }
                        )
                    }

                    // Rating Section
                    item {

                        RatingSection(
                            averageRating = averageRating,
                            totalRatings = ratingsSize,
                            currentUserRatingValue = userRating?.rating,
                            onRatingSubmitted = { ratingValue ->
                                viewModel.submitRating(currentEvent.id!!, ratingValue)
                            },
                            enabledRatingButton = viewModel.enabledRatingButton.collectAsState().value
                        )
                    }

                    // Comments Section
                    item {
                        CommentsSection(
                            comments = comments,
                            onAddComment = { commentText ->
                                viewModel.addComment(currentEvent.id!!, commentText)
                            }
                        )
                    }

                    // Error display at the bottom
                    error?.let {
                        item {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) } // Bottom padding
                }
            }
        }
    }
}

@Composable
fun RsvpSection(
    rsvpData: EventRSPV?,
    currentUserId: String?,
    onRsvpChanged: (RsvpStatus) -> Unit
) {
    val comingCount = rsvpData?.coming_users?.size ?: 0
    val maybeCount = rsvpData?.maybe_users?.size ?: 0
    val notComingCount = rsvpData?.not_coming_users?.size ?: 0

    val userIsComing = rsvpData?.coming_users?.any { it.userId == currentUserId } == true
    val userIsMaybe = rsvpData?.maybe_users?.any { it.userId == currentUserId } == true
    val userIsNotComing = rsvpData?.not_coming_users?.any { it.userId == currentUserId } == true

    Column {
        Text("RSVP", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onRsvpChanged(RsvpStatus.COMING) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userIsComing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.weight(1f)
            ) { Text("Coming (${comingCount})") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onRsvpChanged(RsvpStatus.MAYBE) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userIsMaybe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.weight(1f)
            ) { Text("Maybe (${maybeCount})") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onRsvpChanged(RsvpStatus.NOT_COMING) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userIsNotComing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.weight(1f)
            ) { Text("Not Coming (${notComingCount})") }
        }
    }
}

@Composable
fun RatingSection(
    averageRating: Float,
    totalRatings: Int,
    currentUserRatingValue: Float?,
    onRatingSubmitted: (Float) -> Unit,
    enabledRatingButton: Boolean
) {
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedUserRating by remember { mutableStateOf(currentUserRatingValue ?: 3f) } // Default to 3 for dialog

    LaunchedEffect(currentUserRatingValue) { // Update dialog's rating if external rating changes
        currentUserRatingValue?.let { selectedUserRating = it }
    }


    Column {
        Text("Ratings", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = "Average Rating", tint = Color(0xFFFFC107)) // Yellow star
            Text(String.format(Locale.US, "%.1f", averageRating), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(" ($totalRatings ratings)", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { showRatingDialog = enabledRatingButton }, enabled = enabledRatingButton) {
            Text(if (currentUserRatingValue != null) "Update Your Rating" else "Rate this Event")
        }
    }

    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("Rate this Event") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your rating: ${String.format(Locale.US, "%.1f", selectedUserRating)}")
                    Slider(
                        value = selectedUserRating,
                        onValueChange = { selectedUserRating = it },
                        valueRange = 1f..5f,
                        steps = 8 // 0.5 steps ( (5-1) / 0.5 = 8 steps )
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1.0")
                        Text("5.0")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRatingSubmitted(selectedUserRating)
                    showRatingDialog = false
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CommentsSection(
    comments: List<EventComment>,
    onAddComment: (String) -> Unit
) {
    var commentInput by remember { mutableStateOf(TextFieldValue("")) }

    Column {
        Text("Comments", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = commentInput,
            onValueChange = { commentInput = it },
            label = { Text("Add a comment...") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    if (commentInput.text.isNotBlank()) {
                        onAddComment(commentInput.text)
                        commentInput = TextFieldValue("") // Clear input
                    }
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Post Comment")
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (comments.isEmpty()) {
            Text("No comments yet. Be the first to comment!", style = MaterialTheme.typography.bodySmall)
        } else {
            comments.forEach { comment ->
                CommentItem(comment)
                Divider()
            }
        }
    }
}

@Composable
fun CommentItem(comment: EventComment) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        comment.profileImageUrl?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "${comment.userName}'s profile picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } ?: Icon(
            Icons.Filled.AccountCircle,
            contentDescription = "User Avatar",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(comment.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(comment.commentText, style = MaterialTheme.typography.bodySmall)
            Text(formatter.format(comment.createdAt.toDate()), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}