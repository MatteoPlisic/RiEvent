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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.R
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
    val uiState by viewModel.uiState.collectAsState()
    val allEventsViewModel: AllEventsViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.loadEvents(currentUserId)
    }

    Drawer(
        title = stringResource(id = R.string.my_events_title),
        navController = navController,
        gesturesEnabled = true,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(id = R.string.my_events_title)) })
            },
        ) { scaffoldPadding ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.myEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.my_events_no_events))
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

            uiState.eventToDelete?.let { event ->
                AlertDialog(
                    onDismissRequest = { viewModel.onDeletionDismissed() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onDeletionConfirmed() }) {
                            Text(stringResource(id = R.string.my_events_delete_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onDeletionDismissed() }) {
                            Text(stringResource(id = R.string.dialog_cancel_button))
                        }
                    },
                    title = { Text(stringResource(id = R.string.my_events_delete_dialog_title)) },
                    text = { Text(stringResource(id = R.string.my_events_delete_dialog_text, event.name)) }
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
            AllEventCard(
                event = event,
                allEventsViewModel = allEventsViewModel,
                onCardClick = onEventClick
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.my_events_edit_icon_description), modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(id = R.string.my_events_edit_button))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDeleteClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.my_events_delete_icon_description), modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(id = R.string.my_events_delete_button))
                }
            }
        }
    }
}