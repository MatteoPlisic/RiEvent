package com.example.rievent.ui.allevents

import Event
import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Slider
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllEventsScreen(
    viewModel: AllEventsViewModel = viewModel(),
    navController: NavController
) {
    val events by viewModel.events.collectAsState()
    val context = LocalContext.current

    // States for filters
    var searchText by remember { mutableStateOf("") }
    var searchByUser by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Any") }
    val categoryOptions = listOf("Any", "Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    // States for location and distance filter
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var distanceFilterKm by remember { mutableStateOf(50f) } // Default to 50km

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted, get the current location
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location: Location? ->
                    userLocation = location
                    Log.d("Location", "Location acquired: $location")
                }.addOnFailureListener {
                    Log.e("Location", "Failed to get location", it)
                }
            } else {
                Log.d("Location", "Location permission denied by user.")
            }
        }
    )

    // Date Picker state and dialog
    val datePickerState = rememberDatePickerState()
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

    // Initial data load and permission request
    LaunchedEffect(Unit) {
        viewModel.loadAllPublicEvents()
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Trigger search whenever a filter changes
    LaunchedEffect(searchText, searchByUser, selectedCategory, selectedDate, distanceFilterKm, userLocation) {
        viewModel.search(searchText, searchByUser, selectedCategory, selectedDate, distanceFilterKm, userLocation)
    }

    // Collect navigation actions from ViewModel
    LaunchedEffect(key1 = viewModel.navigateToSingleEventAction) {
        viewModel.navigateToSingleEventAction.collect { eventId ->
            navController.navigate("singleEvent/$eventId")
        }
    }

    Drawer(
        title = "Home",
        gesturesEnabled = true,
        navController = navController
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Search TextField
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search Event/User") },
                trailingIcon = { Icon(if (searchByUser) Icons.Default.Person else Icons.Default.Search, "Search Mode") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Category and User/Event filters
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategory) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        categoryOptions.forEach { category ->
                            DropdownMenuItem(text = { Text(category) }, onClick = {
                                selectedCategory = category
                                expandedCategory = false
                            })
                        }
                    }
                }
                FilterChip(selected = !searchByUser, onClick = { searchByUser = false }, label = { Text("By Event", maxLines = 1) })
                FilterChip(selected = searchByUser, onClick = { searchByUser = true }, label = { Text("By User", maxLines = 1) })
            }
            Spacer(modifier = Modifier.height(8.dp))


            // Date Filter TextField
            OutlinedTextField(
                value = selectedDate?.format(dateFormatter) ?: "Any Date",
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter by Date") },
                modifier = Modifier.fillMaxWidth().clickable { showDatePickerDialog = true },
                trailingIcon = {
                    if (selectedDate != null) {
                        IconButton(onClick = { selectedDate = null }) {
                            Icon(Icons.Filled.Clear, "Clear Date")
                        }
                    } else {
                        IconButton(onClick = { showDatePickerDialog = true }) {
                            Icon(Icons.Filled.DateRange, "Select Date")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Distance Slider
            Text(
                text = "Distance: " + if (distanceFilterKm >= 50f) {
                    "Any"
                } else {
                    String.format(Locale.US, "within %.0f km", distanceFilterKm)
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
            Slider(
                value = distanceFilterKm,
                onValueChange = { distanceFilterKm = it },
                valueRange = 1f..50f,
                steps = 48,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Event List
            if (events.isEmpty() && (searchText.isNotEmpty() || selectedCategory != "Any" || selectedDate != null || distanceFilterKm < 50f)) {
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
    val eventRsvpData: EventRSPV? = remember(event.id, rsvpMap) { event.id?.let { rsvpMap[it] } }
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    DisposableEffect(event.id, allEventsViewModel) {
        val currentEventId = event.id
        if (!currentEventId.isNullOrBlank()) {
            allEventsViewModel.listenToRsvpForEvent(currentEventId)
        }
        onDispose {
            if (!currentEventId.isNullOrBlank()) {
                allEventsViewModel.stopListeningToRsvp(currentEventId)
            }
        }
    }

    val (thisUserComing, thisUserMaybeComing, thisUserNotComing) = remember(eventRsvpData, currentUid) {
        if (currentUid == null || eventRsvpData == null) Triple(false, false, false)
        else Triple(
            eventRsvpData.coming_users.any { it.userId == currentUid },
            eventRsvpData.maybe_users.any { it.userId == currentUid },
            eventRsvpData.not_coming_users.any { it.userId == currentUid }
        )
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(event.name, style = MaterialTheme.typography.titleLarge)
                Text("Category: ${event.category}", style = MaterialTheme.typography.bodyMedium)
                Text("By: ${event.ownerName ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                val eventDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, HH:mm") }
                event.startTime?.let { Text("Starts: ${it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(eventDateFormatter)}", style = MaterialTheme.typography.bodySmall) }
                event.endTime?.let { Text("Ends: ${it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(eventDateFormatter)}", style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.COMING) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (thisUserComing) Color(0xFF66BB6A) else MaterialTheme.colorScheme.primaryContainer, contentColor = if (thisUserComing) Color.White else MaterialTheme.colorScheme.onPrimaryContainer), enabled = !thisUserComing) { Text("Coming\n${comingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center) }
                    Button(onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.MAYBE) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (thisUserMaybeComing) Color(0xFFFFCA28) else MaterialTheme.colorScheme.secondaryContainer, contentColor = if (thisUserMaybeComing) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer), enabled = !thisUserMaybeComing) { Text("Maybe\n${maybeComingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center) }
                    Button(onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.NOT_COMING) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (thisUserNotComing) Color(0xFFEF5350) else MaterialTheme.colorScheme.errorContainer, contentColor = if (thisUserNotComing) Color.White else MaterialTheme.colorScheme.onErrorContainer), enabled = !thisUserNotComing) { Text("Not Coming\n${notComingCount}", fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center) }
                }
            }
            event.imageUrl?.let { imageUrl ->
                Image(
                    painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(data = imageUrl).crossfade(true).build()),
                    contentDescription = "Event Icon",
                    modifier = Modifier.size(width = 100.dp, height = 70.dp).padding(top = 8.dp, end = 8.dp).align(Alignment.TopEnd),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}