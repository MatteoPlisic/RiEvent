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
import com.example.rievent.ui.utils.Drawer

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
                        onImComing   = { viewModel.rsvp(event.id, Rsvp.COMING) },
                        onMaybe      = { viewModel.rsvp(event.id, Rsvp.MAYBE) },
                        onNotComing  = { viewModel.rsvp(event.id, Rsvp.NOT_COMING) }
                    )
                }
            }
        }
    }
}

@Composable
fun AllEventCard(
    event: Event,
    onImComing: () -> Unit,
    onMaybe: () -> Unit,
    onNotComing: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Category: ${event.category}")
            Text("Address: ${event.address}")
            Text("Start: ${event.startTime?.toDate()}")
            Text("End: ${event.endTime?.toDate()}")
            if (!event.isPublic) {
                Text("Private", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImComing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("I'm coming")
                }
                OutlinedButton(
                    onClick = onMaybe,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Maybe")
                }
                TextButton(
                    onClick = onNotComing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Not coming")
                }
            }
        }
    }
}

