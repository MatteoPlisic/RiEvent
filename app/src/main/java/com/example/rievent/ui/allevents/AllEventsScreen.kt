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
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.rievent.models.EventRSPV // Ensure this import is correct
import com.example.rievent.ui.utils.Drawer
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.DisposableEffect

// Added imports for Date Picker
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.clickable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    // State for Date Picker
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }


    LaunchedEffect(Unit) {
        viewModel.loadAllPublicEvents() // Initial load
    }

    // When search parameters change, call viewModel.search
    LaunchedEffect(searchText, searchByUser, selectedCategory, selectedDate) { // Added selectedDate
        viewModel.search(searchText, searchByUser, selectedCategory, selectedDate) // Pass selectedDate
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        yearRange = IntRange(LocalDate.now().year - 5, LocalDate.now().year + 5) // Optional: adjust year range
    )

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePickerDialog = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                .padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search Event/User") },
                trailingIcon = {
                    Icon(
                        imageVector = if (searchByUser) Icons.Default.Person else Icons.Default.Search,
                        contentDescription = "Search Mode Icon"
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp)) // Added spacer

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1.5f),
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
                                }
                            )
                        }
                    }
                }

                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = !searchByUser,
                    onClick = { searchByUser = false },
                    label = { Text("By Event", maxLines = 1) }
                )

                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = searchByUser,
                    onClick = { searchByUser = true },
                    label = { Text("By User", maxLines = 1) }
                )
            }

            // Date Filter Field
            OutlinedTextField(
                value = selectedDate?.format(dateFormatter) ?: "Any Date",
                onValueChange = { /* This is fine for a read-only field */ },
                readOnly = true, // This should generally be fine with Modifier.clickable
                label = { Text("Filter by Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, // Explicitly provide an interaction source
                        indication = null, // Optionally remove ripple if it was interfering
                        onClick = {
                           // Log.d(DATE_PICKER_DEBUG_TAG, "Date TextField MODIFIER onClick fired. Current showDatePickerDialog: $showDatePickerDialog. Setting to true.")
                            showDatePickerDialog = true // This is the crucial line
                        }
                    ),
                trailingIcon = {
                    if (selectedDate != null) {
                        IconButton(onClick = {
                           // Log.d(DATE_PICKER_DEBUG_TAG, "Clear Date Filter IconButton clicked.")
                            selectedDate = null
                            datePickerState.selectedDateMillis = null // Reset DatePicker's internal state
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear Date Filter")
                        }
                    } else {
                        // Make the DateRange icon itself an IconButton to test clickability
                        IconButton(onClick = {
                           // Log.d(DATE_PICKER_DEBUG_TAG, "DateRange ICONBUTTON clicked. Setting showDatePickerDialog = true")
                            showDatePickerDialog = true
                        }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
                        }
                    }
                })
            Spacer(modifier = Modifier.height(16.dp)) // Added spacer


            if (events.isEmpty() && (searchText.isNotEmpty() || selectedCategory != "Any" || selectedDate != null)) {
                Text("No events found matching your criteria.", modifier = Modifier.padding(16.dp))
            } else if (events.isEmpty()) {
                Text("No public events available at the moment.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(events, key = { event -> event.id ?: event.hashCode() }) { event ->
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

    DisposableEffect(event.id, allEventsViewModel) {
        val currentEventId = event.id
        if (currentEventId != null && currentEventId.isNotBlank()) {
            allEventsViewModel.listenToRsvpForEvent(currentEventId)
        }
        onDispose {
            if (currentEventId != null && currentEventId.isNotBlank()) {
                allEventsViewModel.stopListeningToRsvp(currentEventId)
            }
        }
    }

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
            .padding(horizontal = 8.dp), // Consider if this horizontal padding is needed if parent Column has padding
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleLarge)
            Text("Category: ${event.category}", style = MaterialTheme.typography.bodyMedium)
            Text("By: ${event.ownerName ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            // Optionally display event dates:
            val eventDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, HH:mm") }
            event.startTime?.let {
                Text(
                    "Starts: ${it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(eventDateFormatter)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            event.endTime?.let {
                Text(
                    "Ends: ${it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(eventDateFormatter)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }


            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.COMING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (thisUserComing) Color(0xFF66BB6A) else MaterialTheme.colorScheme.primaryContainer, // Use theme colors
                        contentColor = if (thisUserComing) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    enabled = !thisUserComing // Or handle differently if already RSVP'd
                ) {
                    Text("Coming\n${comingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }

                Button(
                    onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.MAYBE) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (thisUserMaybeComing) Color(0xFFFFCA28) else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (thisUserMaybeComing) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    enabled = !thisUserMaybeComing
                ) {
                    Text("Maybe\n${maybeComingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }

                Button(
                    onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.NOT_COMING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (thisUserNotComing) Color(0xFFEF5350) else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (thisUserNotComing) Color.White else MaterialTheme.colorScheme.onErrorContainer
                    ),
                    enabled = !thisUserNotComing
                ) {
                    Text("Not Coming\n${notComingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}