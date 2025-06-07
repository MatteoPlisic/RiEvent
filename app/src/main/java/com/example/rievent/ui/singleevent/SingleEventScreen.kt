package com.example.rievent.ui.singleevent

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rievent.R
import com.example.rievent.models.EventComment
import com.example.rievent.models.EventRSPV
import com.example.rievent.ui.allevents.RsvpStatus
import com.example.rievent.ui.utils.Drawer
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleEventScreen(
    eventId: String,
    onBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    navController: NavController,
    viewModel: SingleEventViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    LaunchedEffect(eventId) {
        if (eventId.isNotBlank()) {
            viewModel.loadEventData(eventId)
        }
    }


    Drawer(
        title = uiState.event?.name ?: stringResource(id = R.string.single_event_title),
        navController = navController,
        gesturesEnabled = true

    ) { paddingValues ->
        if (uiState.isLoading && uiState.event == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.event == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage ?: stringResource(id = R.string.single_event_error_not_found),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val event = uiState.event!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Use padding from the Drawer
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                event.imageUrl?.let {
                    item {
                        Image(
                            painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(it).crossfade(true).build()),
                            contentDescription = stringResource(id = R.string.event_image_description),
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item {
                    Text(event.name, style = MaterialTheme.typography.headlineMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.single_event_by_label, event.ownerName), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onNavigateToUserProfile(event.ownerId) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Person, stringResource(id = R.string.single_event_view_owner_profile_description))
                        }
                    }
                    val formatter = remember { SimpleDateFormat("EEE, dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                    event.startTime?.let { Text(stringResource(id = R.string.single_event_starts_label, formatter.format(it.toDate())), style = MaterialTheme.typography.bodyMedium) }
                    event.endTime?.let { Text(stringResource(id = R.string.single_event_ends_label, formatter.format(it.toDate())), style = MaterialTheme.typography.bodyMedium) }
                    Text(stringResource(id = R.string.single_event_category_label, event.category), style = MaterialTheme.typography.bodyMedium)
                    Text(event.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                    Text(stringResource(id = R.string.single_event_address_label, event.address), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
                item { Divider() }
                item {
                    RsvpSection(
                        rsvpData = uiState.rsvp,
                        currentUserId = currentUid,
                        onRsvpChanged = { newStatus -> viewModel.updateRsvp(event.id!!, newStatus) }
                    )
                }
                item { Divider() }
                item {
                    RatingSection(
                        averageRating = uiState.averageRating,
                        totalRatings = uiState.totalRatings,
                        currentUserRatingValue = uiState.userRating,
                        isRatingEnabled = uiState.isRatingEnabled,
                        isRatingDialogVisible = uiState.isRatingDialogVisible,
                        onRatingSubmitted = { ratingValue -> viewModel.submitRating(event.id!!, ratingValue) },
                        onDialogToggled = { viewModel.onRatingDialogToggled(it) }
                    )
                }
                item { Divider() }
                item {
                    CommentsSection(
                        comments = uiState.comments,
                        commentText = uiState.newCommentText,
                        onCommentTextChange = { viewModel.onNewCommentChange(it) },
                        onAddComment = { viewModel.addComment(event.id!!) }
                    )
                }
                uiState.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
@Composable
fun RsvpSection(rsvpData: EventRSPV?, currentUserId: String?, onRsvpChanged: (RsvpStatus) -> Unit) {
    val comingCount = rsvpData?.coming_users?.size ?: 0
    val maybeCount = rsvpData?.maybe_users?.size ?: 0
    val notComingCount = rsvpData?.not_coming_users?.size ?: 0
    val userIsComing = rsvpData?.coming_users?.any { it.userId == currentUserId } == true
    val userIsMaybe = rsvpData?.maybe_users?.any { it.userId == currentUserId } == true
    val userIsNotComing = rsvpData?.not_coming_users?.any { it.userId == currentUserId } == true

    Column {
        Text(stringResource(id = R.string.rsvp_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onRsvpChanged(RsvpStatus.COMING) }, colors = ButtonDefaults.buttonColors(containerColor = if (userIsComing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1f)) { Text(stringResource(id = R.string.rsvp_coming_button, comingCount)) }
            Button(onClick = { onRsvpChanged(RsvpStatus.MAYBE) }, colors = ButtonDefaults.buttonColors(containerColor = if (userIsMaybe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1f)) { Text(stringResource(id = R.string.rsvp_maybe_button, maybeCount)) }
            Button(onClick = { onRsvpChanged(RsvpStatus.NOT_COMING) }, colors = ButtonDefaults.buttonColors(containerColor = if (userIsNotComing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.rsvp_not_coming_button, notComingCount),
                    textAlign = TextAlign.Center,
                    fontSize = MaterialTheme.typography.labelMedium.fontSize
                )
            }
        }
    }
}

@Composable
fun RatingSection(
    averageRating: Float, totalRatings: Int, currentUserRatingValue: Float?,
    isRatingEnabled: Boolean, isRatingDialogVisible: Boolean,
    onRatingSubmitted: (Float) -> Unit, onDialogToggled: (Boolean) -> Unit
) {
    var selectedUserRating by remember { mutableStateOf(currentUserRatingValue ?: 3f) }
    LaunchedEffect(currentUserRatingValue) { currentUserRatingValue?.let { selectedUserRating = it } }

    Column {
        Text(stringResource(id = R.string.ratings_title), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, stringResource(id = R.string.ratings_average_description), tint = Color(0xFFFFC107))
            Text(String.format(Locale.US, "%.1f", averageRating), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(id = R.string.ratings_count_label, totalRatings), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onDialogToggled(true) }, enabled = isRatingEnabled) {
            Text(if (currentUserRatingValue != null) stringResource(id = R.string.ratings_update_button) else stringResource(id = R.string.ratings_rate_button))
        }
    }
    if (isRatingDialogVisible) {
        AlertDialog(
            onDismissRequest = { onDialogToggled(false) },
            title = { Text(stringResource(id = R.string.ratings_dialog_title)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(id = R.string.ratings_dialog_your_rating, selectedUserRating))
                    Slider(value = selectedUserRating, onValueChange = { selectedUserRating = it }, valueRange = 1f..5f, steps = 8)
                }
            },
            confirmButton = { TextButton(onClick = { onRatingSubmitted(selectedUserRating) }) { Text(stringResource(id = R.string.dialog_submit_button)) } },
            dismissButton = { TextButton(onClick = { onDialogToggled(false) }) { Text(stringResource(id = R.string.dialog_cancel_button)) } }
        )
    }
}

@Composable
fun CommentsSection(
    comments: List<EventComment>, commentText: String,
    onCommentTextChange: (String) -> Unit, onAddComment: () -> Unit
) {
    Column {
        Text(stringResource(id = R.string.comments_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = commentText,
            onValueChange = onCommentTextChange,
            label = { Text(stringResource(id = R.string.comments_add_label)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { IconButton(onClick = onAddComment) { Icon(Icons.Filled.Send, stringResource(id = R.string.comments_post_description)) } }
        )
        Spacer(Modifier.height(16.dp))
        if (comments.isEmpty()) {
            Text(stringResource(id = R.string.comments_no_comments), style = MaterialTheme.typography.bodySmall)
        } else {
            comments.forEach { comment -> CommentItem(comment); Divider() }
        }
    }
}

@Composable
fun CommentItem(comment: EventComment) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    Row(Modifier.padding(vertical = 8.dp)) {
        if (comment.profileImageUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(model = comment.profileImageUrl),
                contentDescription = stringResource(id = R.string.comment_item_profile_picture_description, comment.userName),
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(id = R.string.comment_item_default_avatar_description),
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(comment.userName, fontWeight = FontWeight.Bold)
            Text(comment.commentText, style = MaterialTheme.typography.bodySmall)
            Text(formatter.format(comment.createdAt.toDate()), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}