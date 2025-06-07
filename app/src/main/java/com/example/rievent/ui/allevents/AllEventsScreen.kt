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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rievent.R
import com.example.rievent.models.EventRSPV
import com.example.rievent.ui.utils.Drawer
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllEventsScreen(
    viewModel: AllEventsViewModel = viewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val categoryOptions = listOf("Any", "Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                    .addOnSuccessListener { location: Location? ->
                        viewModel.onUserLocationUpdated(location)
                        Log.d("Location", "Location acquired: $location")
                    }.addOnFailureListener {
                        Log.e("Location", "Failed to get location", it)
                    }
            } else {
                Log.d("Location", "Location permission denied by user.")
            }
        }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(key1 = viewModel.navigateToSingleEventAction) {
        viewModel.navigateToSingleEventAction.collect { eventId ->
            navController.navigate("singleEvent/$eventId")
        }
    }

    val datePickerState = rememberDatePickerState()
    if (uiState.isDatePickerDialogVisible) {
        DatePickerDialog(
            onDismissRequest = { viewModel.onDatePickerDialogDismissed() },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.onDateSelected(date)
                    } ?: viewModel.onDatePickerDialogDismissed()
                }) { Text(stringResource(id = R.string.dialog_submit_button)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDatePickerDialogDismissed() }) { Text(stringResource(id = R.string.dialog_cancel_button)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Drawer(title = stringResource(id = R.string.all_events_title), gesturesEnabled = true, navController = navController) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {

            OutlinedTextField(
                value = uiState.searchText,
                onValueChange = { viewModel.onSearchTextChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.all_events_search_label)) },
                trailingIcon = { Icon(if (uiState.searchByUser) Icons.Default.Person else Icons.Default.Search, stringResource(id = R.string.all_events_search_mode_description)) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1.5f),
                    expanded = uiState.isCategoryMenuExpanded,
                    onExpandedChange = { viewModel.onCategoryMenuToggled(it) }
                ) {
                    TextField(
                        value = uiState.selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.category_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(uiState.isCategoryMenuExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = uiState.isCategoryMenuExpanded, onDismissRequest = { viewModel.onCategoryMenuToggled(false) }) {
                        categoryOptions.forEach { category ->
                            DropdownMenuItem(text = { Text(category) }, onClick = { viewModel.onCategorySelected(category) })
                        }
                    }
                }
                FilterChip(selected = !uiState.searchByUser, onClick = { viewModel.onSearchModeChanged(false) }, label = { Text(stringResource(id = R.string.all_events_filter_by_event), maxLines = 1) })
                FilterChip(selected = uiState.searchByUser, onClick = { viewModel.onSearchModeChanged(true) }, label = { Text(stringResource(id = R.string.all_events_filter_by_user), maxLines = 1) })
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.selectedDate?.format(dateFormatter) ?: stringResource(id = R.string.map_screen_any_date),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(id = R.string.map_screen_filter_by_date)) },
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onDatePickerDialogOpened() },
                trailingIcon = {
                    if (uiState.selectedDate != null) {
                        IconButton(onClick = { viewModel.onDateSelected(null) }) { Icon(Icons.Filled.Clear, stringResource(id = R.string.map_screen_clear_date_filter)) }
                    } else {
                        IconButton(onClick = { viewModel.onDatePickerDialogOpened() }) { Icon(Icons.Filled.DateRange, stringResource(id = R.string.map_screen_select_date)) }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            val distanceText = if (uiState.distanceFilterKm >= 50f) {
                stringResource(id = R.string.all_events_distance_any)
            } else {
                stringResource(id = R.string.all_events_distance_within_km, uiState.distanceFilterKm)
            }
            Text(
                text = stringResource(id = R.string.all_events_distance_label, distanceText),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
            Slider(
                value = uiState.distanceFilterKm,
                onValueChange = { viewModel.onDistanceChanged(it) },
                valueRange = 1f..50f,
                steps = 48,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.displayedEvents.isEmpty() && uiState.hasAppliedFilters) {
                Text(stringResource(id = R.string.all_events_no_results), modifier = Modifier.padding(16.dp))
            } else if (uiState.displayedEvents.isEmpty()) {
                Text(stringResource(id = R.string.all_events_no_public_events), modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(uiState.displayedEvents, key = { event -> event.id ?: event.hashCode() }) { event ->
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
    val uiState by allEventsViewModel.uiState.collectAsState()
    val rsvpMap = uiState.eventsRsvpsMap
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { event.id?.let { onCardClick(it) } },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(event.name, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(id = R.string.single_event_category_label, event.category), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(id = R.string.single_event_by_label, event.ownerName ?: stringResource(id = R.string.all_events_n_a)), style = MaterialTheme.typography.bodySmall)
                val eventDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, HH:mm") }
                event.startTime?.let { Text(stringResource(id = R.string.single_event_starts_label, it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(eventDateFormatter)), style = MaterialTheme.typography.bodySmall) }
                event.endTime?.let { Text(stringResource(id = R.string.single_event_ends_label, it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(eventDateFormatter)), style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.COMING) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (thisUserComing) Color(0xFF66BB6A) else MaterialTheme.colorScheme.primaryContainer, contentColor = if (thisUserComing) Color.White else MaterialTheme.colorScheme.onPrimaryContainer), enabled = !thisUserComing) { Text(stringResource(id = R.string.all_events_card_coming_button, comingCount), fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center) }
                    Button(onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.MAYBE) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (thisUserMaybeComing) Color(0xFFFFCA28) else MaterialTheme.colorScheme.secondaryContainer, contentColor = if (thisUserMaybeComing) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer), enabled = !thisUserMaybeComing) { Text(stringResource(id = R.string.all_events_card_maybe_button, maybeComingCount), fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center) }
                    Button(onClick = { allEventsViewModel.updateRsvp(event.id, RsvpStatus.NOT_COMING) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (thisUserNotComing) Color(0xFFEF5350) else MaterialTheme.colorScheme.errorContainer, contentColor = if (thisUserNotComing) Color.White else MaterialTheme.colorScheme.onErrorContainer), enabled = !thisUserNotComing) { Text(stringResource(id = R.string.all_events_card_not_coming_button, notComingCount), fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center) }
                }
            }
            event.imageUrl?.let { imageUrl ->
                Image(
                    painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(data = imageUrl).crossfade(true).build()),
                    contentDescription = stringResource(id = R.string.all_events_card_icon_description),
                    modifier = Modifier.size(width = 100.dp, height = 70.dp).padding(top = 8.dp, end = 8.dp).align(Alignment.TopEnd),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}