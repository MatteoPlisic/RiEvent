package com.example.rievent.ui.allevents

// Added imports for Date Picker
import Event
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rievent.models.EventRSPV
import com.example.rievent.ui.utils.Drawer
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllEventsScreen(
    viewModel: AllEventsViewModel = viewModel(),
    navController: NavController
) {
    val events by viewModel.events.collectAsState()

    // ... (searchText, searchByUser, selectedCategory, selectedDate, datePickerState states remain)
    var searchText by remember { mutableStateOf("") }
    var searchByUser by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Any") }
    val categoryOptions = listOf("Any","Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = null,//selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        yearRange = IntRange(LocalDate.now().year - 10, LocalDate.now().year + 10)
    )
    // ... (datePicker dialog remains the same)
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


    LaunchedEffect(Unit) {
        viewModel.loadAllPublicEvents() // Initial load
    }

    LaunchedEffect(searchText, searchByUser, selectedCategory, selectedDate) {
        viewModel.search(searchText, searchByUser, selectedCategory, selectedDate)
    }

    // Collect navigation actions from ViewModel
    LaunchedEffect(key1 = viewModel.navigateToSingleEventAction) {
        viewModel.navigateToSingleEventAction.collect { eventId ->
            Log.d("AllEventsScreen", "Collected navigation action for eventId: $eventId")
            navController.navigate("singleEvent/$eventId")
        }
    }

    Drawer(
        title = "Home",
        navController = navController
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // ... (OutlinedTextField for search, Row for category and filter chips, OutlinedTextField for date filter remain the same)
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
            Spacer(modifier = Modifier.height(8.dp))

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

            OutlinedTextField(
                value = selectedDate?.format(dateFormatter) ?: "Any Date",
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter by Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePickerDialog = true
                        Log.d("DatePicker", "TextField clicked, showDatePickerDialog = true")},
                trailingIcon = {
                    if (selectedDate != null) {
                        IconButton(onClick = { selectedDate = null; showDatePickerDialog = true }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear Date Filter")
                        }
                    } else {
                        IconButton(onClick = {
                            showDatePickerDialog = true
                            Log.d("DatePicker", "DateRange icon clicked, showDatePickerDialog = true")
                        }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))


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
                            allEventsViewModel = viewModel,
                            onCardClick = { eventId -> viewModel.onEventClicked(eventId) }

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
    modifier: Modifier = Modifier,
    onCardClick: (eventId: String) -> Unit
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
            .padding(horizontal = 8.dp)
            .clickable { event.id?.let { onCardClick(it) } },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),

    ) {
        // Use a Box to layer the small image on top of the content
        Box(modifier = Modifier.fillMaxWidth()) {
            // Main content Column (everything except the small image)
            Column(modifier = Modifier.padding(16.dp)) {
                // Add some top padding to make space for the image if it overlaps,
                // or adjust image padding below. For top-right, text might flow under it naturally.
                // Spacer(modifier = Modifier.height(20.dp)) // Optional: if image is large and you want text below

                Text(event.name, style = MaterialTheme.typography.titleLarge)
                Text("Category: ${event.category}", style = MaterialTheme.typography.bodyMedium)
                Text("By: ${event.ownerName ?: "N/A"}", style = MaterialTheme.typography.bodySmall)

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
                    // ... RSVP Buttons (unchanged) ...
                    Button(
                        onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.COMING) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (thisUserComing) Color(0xFF66BB6A) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (thisUserComing) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        enabled = !thisUserComing
                    ) {
                        Text("Coming\n${comingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
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
                        Text("Maybe\n${maybeComingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
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
                        Text("Not Coming\n${notComingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            // Small Event Image in the Top Right Corner
            event.imageUrl?.let { imageUrl ->
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(data = imageUrl)
                            .apply {
                                crossfade(true)

                                // placeholder(R.drawable.placeholder_image_small)
                                // error(R.drawable.error_image_small)
                            }.build()
                    ),
                    contentDescription = "Event Icon",
                    modifier = Modifier
                        .size(width = 100.dp, height = 70.dp) // Adjust size as needed for the small image
                        .padding(top = 8.dp, end = 8.dp) // Padding from the card edges
                        .align(Alignment.TopEnd), // Align to the top right of the Box

                    contentScale = ContentScale.Crop
                )
            }
            // Optional: Placeholder if no image
            // else {
            //     Icon(
            //         imageVector = Icons.Default.Event, // Or your preferred placeholder
            //         contentDescription = "Event Icon Placeholder",
            //         modifier = Modifier
            //             .size(56.dp)
            //             .padding(top = 8.dp, end = 8.dp)
            //             .align(Alignment.TopEnd)
            //             .clip(CircleShape),
            //         tint = MaterialTheme.colorScheme.primary
            //     )
            // }
        }
    }
}
