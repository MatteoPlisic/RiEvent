package com.example.rievent.ui.myevents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import Event
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Edit
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(viewModel: MyEventsViewModel = viewModel(),navController: NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val events by viewModel.events.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadEvents(currentUserId)
    }

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
                EventCard(
                    event = event,
                    onDeleteClick = {
                        eventToDelete = event
                        showDialog = true
                    },
                    onEditClick = {
                        navController.navigate("updateEvent/${event.id}")
                    }
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

@Composable
fun EventCard(
    event: Event,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }


            Spacer(modifier = Modifier.height(8.dp))
            Text("Category: ${event.category}")
            Text("Address: ${event.address}")
            Text("Start: ${event.startTime?.toDate()}")
            Text("End: ${event.endTime?.toDate()}")
            if (!event.isPublic) {
                Text("Private", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
