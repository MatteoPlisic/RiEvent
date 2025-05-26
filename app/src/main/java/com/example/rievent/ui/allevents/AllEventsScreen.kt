package com.example.rievent.ui.allevents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import Event // Assuming Event data class is in the root package or correctly imported
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.rievent.models.EventRSPV // Ensure this import is correct
import com.example.rievent.ui.utils.Drawer
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllEventsScreen(viewModel: AllEventsViewModel = viewModel(),
                    onLogout: () -> Unit,
                    onNavigateToProfile: () -> Unit,
                    onNavigateToEvents: () -> Unit,
                    onNavigateToCreateEvent: () -> Unit,
                    onNavigateToMyEvents: () -> Unit) {
    val events by viewModel.events.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var searchByUser by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Any") }
    val categoryOptions = listOf("Any","Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")

    LaunchedEffect(Unit) {
        viewModel.loadAllPublicEvents() // Initial load
    }

    // When search parameters change, call viewModel.search
    // This ensures that if any parameter changes, the search is re-triggered.
    LaunchedEffect(searchText, searchByUser, selectedCategory) {
        viewModel.search(searchText, searchByUser, selectedCategory)
    }


    Drawer(
        title = "Home",
        onLogout = onLogout,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToEvents = onNavigateToEvents,
        onNavigateToCreateEvent = onNavigateToCreateEvent,
        onNavigateToMyEvents = onNavigateToMyEvents,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 16.dp) // Added padding

        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it /* LaunchedEffect above will trigger search */ },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search") },
                trailingIcon = {
                    Icon(
                        imageVector = if (searchByUser) Icons.Default.Person else Icons.Default.Search,
                        contentDescription = "Search Mode"
                    )
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically // Align items in row
            ) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1.5f), // Give more space to dropdown
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    TextField(
                        value = selectedCategory,
                        onValueChange = { /* no‐op */ },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategory) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categoryOptions.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                    // LaunchedEffect above will trigger search
                                }
                            )
                        }
                    }
                }

                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = !searchByUser,
                    onClick = {
                        searchByUser = false
                        // LaunchedEffect above will trigger search
                    },
                    label = { Text("By Event", maxLines = 1) } // Prevent text wrapping issues
                )

                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = searchByUser,
                    onClick = {
                        searchByUser = true
                        // LaunchedEffect above will trigger search
                    },
                    label = { Text("By User", maxLines = 1) } // Prevent text wrapping issues
                )
            }

            if (events.isEmpty() && searchText.isNotEmpty()) {
                Text("No events found matching your criteria.", modifier = Modifier.padding(16.dp))
            } else if (events.isEmpty()) {
                Text("No public events available at the moment.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp) // Padding at the bottom of the list
                ) {
                    items(events, key = { event -> event.id ?: event.hashCode() }) { event -> // Use a stable key
                        AllEventCard(
                            event = event,
                            allEventsViewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AllEventCard(
    event: Event,
    allEventsViewModel: AllEventsViewModel,
    modifier: Modifier = Modifier
) {
    val rsvpMap by allEventsViewModel.eventsRsvpsMap.collectAsState()
    val eventRsvpData: EventRSPV? = remember(event.id, rsvpMap) {
        event.id?.let { rsvpMap[it] }
    }

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    // Manage listener lifecycle for this card's event using DisposableEffect
    DisposableEffect(event.id, allEventsViewModel) { // Keys that trigger re-running the effect
        val currentEventId = event.id // Capture event.id for use in onDispose

        if (currentEventId != null && currentEventId.isNotBlank()) {
            allEventsViewModel.listenToRsvpForEvent(currentEventId)
        }

        // onDispose is the cleanup lambda provided by DisposableEffectScope
        onDispose {
            if (currentEventId != null && currentEventId.isNotBlank()) {
                allEventsViewModel.stopListeningToRsvp(currentEventId)
            }
        }
    }

    // Derived state for user's RSVP status for this event
    val (thisUserComing, thisUserMaybeComing, thisUserNotComing) = remember(eventRsvpData, currentUid) {
        if (currentUid == null || eventRsvpData == null) {
            Triple(false, false, false)
        } else {
            Triple(
                eventRsvpData.coming_users.any { it.userId == currentUid },
                eventRsvpData.maybe_users.any { it.userId == currentUid },
                eventRsvpData.not_coming_users.any { it.userId == currentUid }
            )
        }
    }

    val comingCount = eventRsvpData?.coming_users?.size ?: 0
    val maybeComingCount = eventRsvpData?.maybe_users?.size ?: 0
    val notComingCount = eventRsvpData?.not_coming_users?.size ?: 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleLarge)
            Text("Category: ${event.category}", style = MaterialTheme.typography.bodyMedium)
            Text("By: ${event.ownerName ?: "N/A"}", style = MaterialTheme. typography.bodySmall)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.COMING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (thisUserComing) Color(0xFF66BB6A) else Color.Green,
                        contentColor = Color.White
                    ),
                    enabled = !thisUserComing
                ) {
                    Text("Coming\n${comingCount}", fontSize = 11.sp, lineHeight = 12.sp)
                }

                Button(
                    onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.MAYBE) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (thisUserMaybeComing) Color(0xFFFFCA28) else Color.Yellow,
                        contentColor = Color.Black
                    ),
                    enabled = !thisUserMaybeComing
                ) {
                    Text("Maybe\n${maybeComingCount}", fontSize = 11.sp, lineHeight = 12.sp)
                }

                Button(
                    onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.NOT_COMING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (thisUserNotComing) Color(0xFFEF5350) else Color.Red,
                        contentColor = Color.White
                    ),
                    enabled = !thisUserNotComing
                ) {
                    Text("Not Coming\n${notComingCount}", fontSize = 11.sp, lineHeight = 12.sp)
                }
            }
        }
    }
}