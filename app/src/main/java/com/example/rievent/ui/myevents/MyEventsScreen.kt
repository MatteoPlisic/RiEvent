package com.example.rievent.ui.myevents

import Event
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun MyEventsScreen(
    viewModel: MyEventsViewModel = viewModel(),
    navController: NavController,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToMyEvents: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    // The UI now collects a single, clean state object.
    val uiState by viewModel.uiState.collectAsState()

    // This is still needed for the AllEventCard, which is a shared component.
    val allEventsViewModel: AllEventsViewModel = viewModel()

    // Load events when the screen is first composed.
    LaunchedEffect(Unit) {
        viewModel.loadEvents(currentUserId)
    }

    Drawer(
        title = "My Events", // Updated title for clarity
        navController = navController,
        gesturesEnabled = true,
    ) { innerPadding ->
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("My Events") })
            },
            modifier = Modifier.padding(innerPadding)
        ) { scaffoldPadding ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.myEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You have not created any events.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(scaffoldPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.myEvents) { event ->
                        MyEventItemCard(
                            event = event,
                            onDeleteClick = { viewModel.onDeletionInitiated(event) },
                            onEditClick = { navController.navigate("updateEvent/${event.id}") },
                            onEventClick = { eventId -> navController.navigate("singleEvent/$eventId") },
                            allEventsViewModel = allEventsViewModel
                        )
                    }
                }
            }

            // The dialog's visibility is now controlled by the state object.
            uiState.eventToDelete?.let { event ->
                AlertDialog(
                    onDismissRequest = { viewModel.onDeletionDismissed() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onDeletionConfirmed() }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onDeletionDismissed() }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Delete Event") },
                    text = { Text("Are you sure you want to delete \"${event.name}\"?") }
                )
            }
        }
    }
}

@Composable
fun MyEventItemCard(
    event: Event,
    allEventsViewModel: AllEventsViewModel,
    onEventClick: (eventId: String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // AllEventCard is a shared component and can remain as is.
            AllEventCard(
                event = event,
                allEventsViewModel = allEventsViewModel,
                onCardClick = onEventClick
            )

            // Row for Edit and Delete actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Event", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Edit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDeleteClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Event", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Delete")
                }
            }
        }
    }
}