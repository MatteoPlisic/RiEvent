package com.example.rievent.ui.myevents

import Event
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.ui.allevents.AllEventCard
import com.example.rievent.ui.allevents.AllEventsViewModel
import com.example.rievent.ui.utils.Drawer
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(viewModel: MyEventsViewModel = viewModel(),
                   navController: NavController,
                   onLogout: () -> Unit,
                   onNavigateToProfile: () -> Unit,
                   onNavigateToEvents: () -> Unit,
                   onNavigateToCreateEvent: () -> Unit,
                   onNavigateToMyEvents: () -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val events by viewModel.events.collectAsState()
    val allEventsViewModel: AllEventsViewModel = viewModel()
    var showDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }


    LaunchedEffect(Unit) {
        viewModel.loadEvents(currentUserId)

    }
    Drawer(
        title = "Home",
        navController = navController
    ){ innerPadding ->


            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("My Events") })
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(events) { event ->
                        MyEventItemCard(
                            event = event,
                            onDeleteClick = {
                                eventToDelete = event
                                showDialog = true
                            },
                            onEditClick = {
                                navController.navigate("updateEvent/${event.id}")
                            },
                            onEventClick = { eventId ->
                                navController.navigate("singleEvent/$eventId")
                            },
                            allEventsViewModel = allEventsViewModel
                        )
                    }
                }

                // ✅ Place it here, *inside* Scaffold but *outside* LazyColumn
                if (showDialog && eventToDelete != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showDialog = false
                            eventToDelete = null
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                eventToDelete?.id?.let { viewModel.deleteEvent(it) }
                                showDialog = false
                                eventToDelete = null
                            }) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDialog = false
                                eventToDelete = null
                            }) {
                                Text("Cancel")
                            }
                        },
                        title = { Text("Delete Event") },
                        text = { Text("Are you sure you want to delete \"${eventToDelete?.name}\"?") }
                    )
                }
            }
        }

}
@Composable
fun MyEventItemCard(
    event: Event,
    allEventsViewModel: AllEventsViewModel, // Pass this if AllEventCard requires it
    onEventClick: (eventId: String) -> Unit, // For when the main card body is clicked
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier // Allow passing a modifier to the outer Card
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // AllEventCard will handle the main display and its own click
            AllEventCard(
                event = event,
                allEventsViewModel = allEventsViewModel,
                onCardClick = { eventId -> onEventClick(eventId) } // Pass through the main card click
            )

            // Row for Edit and Delete actions specific to MyEvents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp), // Adjust padding as needed
                horizontalArrangement = Arrangement.Center, // Align buttons to the right
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onEditClick) {
                    Text("Edit")
                    Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                }
                Spacer(modifier = Modifier.width(4.dp)) // Small spacer between buttons
                Button(onClick = onDeleteClick) {
                    Text("Delete")
                    Icon(Icons.Default.Delete, contentDescription = "Delete Event")
                }
            }
        }
    }
}
