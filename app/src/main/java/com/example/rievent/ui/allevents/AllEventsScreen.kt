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
import Event
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.rievent.models.EventRSPV
import com.example.rievent.ui.utils.Drawer
import com.google.firebase.auth.FirebaseAuth

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
        viewModel.loadAllPublicEvents()
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
                .padding(top = 90.dp)

        ) {

            // Search bar
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.search(it, searchByUser, selectedCategory)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search") },
                trailingIcon = {
                    Icon(
                        imageVector = if (searchByUser) Icons.Default.Person else Icons.Default.Search,
                        contentDescription = "Search Mode"
                    )
                }
            )

            // Toggle between Event and User search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1) Category dropdown
                ExposedDropdownMenuBox(
                    modifier = Modifier.height(48.dp),
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    TextField(
                        value = selectedCategory,
                        onValueChange = { /* no‐op */ },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategory) },
                        modifier = Modifier
                            .menuAnchor()
                            .width(140.dp)
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

                                    viewModel.search(searchText, searchByUser, selectedCategory)
                                }
                            )
                        }
                    }
                }

                // 2) “By Event” chip
                FilterChip(
                    modifier = Modifier.height(48.dp),
                    selected = !searchByUser,
                    onClick = {
                        searchByUser = false
                        viewModel.search(searchText, false, selectedCategory)
                    },
                    label = { Text("By Event") }
                )

                // 3) “By User” chip
                FilterChip(
                    modifier = Modifier.height(48.dp),
                    selected = searchByUser,
                    onClick = {
                        searchByUser = true
                        viewModel.search(searchText, true, selectedCategory)
                    },
                    label = { Text("By User") }
                )
            }


            // List of event cards
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(events) { event ->
                    AllEventCard(
                        event,
                        viewModel
                    )
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
    // 1) A local mutable state to hold the RSVP once it arrives
    var eventRspvList by remember { mutableStateOf<List<EventRSPV>?>(null) }
    var thisUserComing     by remember { mutableStateOf(false) }
    var thisUserMaybeComing by remember { mutableStateOf(false) }
    var thisUserNotComing   by remember { mutableStateOf(false) }
    // 2) Fire off the one‐time callback when the card first composes (or when id changes)
    LaunchedEffect(event.id) {
        allEventsViewModel.rsvp(event.id) { rsvpResult ->
            eventRspvList = rsvpResult
        }
    }

    LaunchedEffect(eventRspvList) {
        // only run when eventRspvList changes from null → data
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        thisUserComing = eventRspvList
            ?.firstOrNull { it.eventId == event.id }
            ?.coming_users
            ?.any { it.userId == currentUid }
            ?: false
        thisUserNotComing = eventRspvList
            ?.firstOrNull { it.eventId == event.id }
            ?.not_coming_users
            ?.any { it.userId == currentUid }
            ?: false

        thisUserMaybeComing = eventRspvList
            ?.firstOrNull { it.eventId == event.id }
            ?.maybe_users
            ?.any { it.userId == currentUid }
            ?: false
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(8.dp))

            var comingCount = 0
            var notComingCount = 0
            var maybeComingCount = 0


            // show a loading indicator until the RSVP comes back
            if (eventRspvList == null) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                comingCount= eventRspvList!!.firstOrNull{ it.eventId == event.id }?.coming_users!!.size
                maybeComingCount = eventRspvList!!.firstOrNull{ it.eventId == event.id }?.maybe_users!!.size
                notComingCount  = eventRspvList!!.firstOrNull{ it.eventId == event.id }?.not_coming_users!!.size




            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // “I’m coming” button – green
                Button(
                    onClick = { thisUserComing = true; thisUserNotComing = false;thisUserMaybeComing = false; allEventsViewModel.updateRsvp(event.id, RsvpStatus.COMING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(!thisUserComing) Color.Green else Color.LightGray,
                        contentColor = Color.White
                    ),
                    enabled = !thisUserComing
                ) {
                    Text("I'm coming:\n ${comingCount}",fontSize = 12.sp)
                }

                // “Maybe” button – yellow
                Button(
                    onClick = { thisUserComing = false; thisUserNotComing = false;thisUserMaybeComing = true; allEventsViewModel.updateRsvp(event.id, RsvpStatus.MAYBE) },
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(!thisUserMaybeComing) Color.Yellow else Color.LightGray,
                        contentColor = Color.Black
                    ),
                    enabled = !thisUserMaybeComing
                ) {
                    Text("Maybe:\n ${maybeComingCount}",fontSize = 12.sp)
                }

                // “Not coming” button – red
                Button(
                    onClick = { thisUserComing = false; thisUserNotComing = true;thisUserMaybeComing = false; allEventsViewModel.updateRsvp(event.id, RsvpStatus.NOT_COMING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(!thisUserNotComing) Color.Red else Color.LightGray ,
                        contentColor = Color.White
                    ),
                    enabled = !thisUserNotComing

                ) {
                    Text("Not coming:\n  ${notComingCount}", fontSize = 12.sp)
                }
            }
        }
    }
}

