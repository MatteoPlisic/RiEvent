package com.example.rievent.ui.map

import EventClusterItem
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.ui.allevents.AllEventCard
import com.example.rievent.ui.allevents.AllEventsViewModel
import com.example.rievent.ui.utils.Drawer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    allEventsViewModel: AllEventsViewModel = viewModel(),
    navController: NavController,
) {
    val allMapEvents by viewModel.allMapEvents.collectAsState()
    val visibleEvents by viewModel.visibleEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedEventId by viewModel.selectedEventId.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    // --- Date Picker States and Dialog ---
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePickerDialog = false
                    datePickerState.selectedDateMillis?.let {
                        val newDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.onDateSelected(newDate)
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

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.3271, 14.4422), 12f)
    }
    val listState = rememberLazyListState()

    val clusterItems = remember(allMapEvents) {
        allMapEvents.map { EventClusterItem(it) }
    }

    LaunchedEffect(allMapEvents) {
        if (allMapEvents.isNotEmpty()) {
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                viewModel.updateVisibleEvents(bounds)
            }
        }
    }


    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                viewModel.updateVisibleEvents(bounds)
            }
        } else {
            viewModel.clearEventSelection()
        }
    }

    LaunchedEffect(selectedEventId) {
        selectedEventId?.let { eventId ->
            allMapEvents.find { it.id == eventId }?.location?.let { geoPoint ->
                val position = LatLng(geoPoint.latitude, geoPoint.longitude)
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(position, 15f),
                    durationMs = 1000
                )
            }
        }
    }

    Drawer(
        title = "Event Map",
        navController = navController,
        gesturesEnabled = false,
    ) { drawerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(drawerPadding)
        ) {
            // --- Date Filter TextField ---
            OutlinedTextField(
                value = selectedDate?.format(dateFormatter) ?: "Any Date",
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter by Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showDatePickerDialog = true },
                trailingIcon = {
                    if (selectedDate != null) {
                        IconButton(onClick = { viewModel.onDateSelected(null) }) {
                            Icon(Icons.Filled.Clear, "Clear Date Filter")
                        }
                    } else {
                        IconButton(onClick = { showDatePickerDialog = true }) {
                            Icon(Icons.Filled.DateRange, "Select Date")
                        }
                    }
                }
            )

            // --- Map Component ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    Clustering(
                        items = clusterItems,
                        onClusterItemInfoWindowClick = { item ->
                            item.event.id?.let { navController.navigate("singleEvent/$it") }
                        },
                        onClusterItemClick = { item ->
                            viewModel.onEventCardSelected(item.event.id)
                            false
                        },
                        onClusterClick = { false }
                    )
                }
            }

            // --- List Component ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading && allMapEvents.isEmpty()) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                } else if (visibleEvents.isEmpty()) {
                    Text(
                        if (selectedDate != null && allMapEvents.isEmpty()) "No events found for this date."
                        else "No events found in this map area."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleEvents, key = { it.id!! }) { event ->
                            Box(
                                modifier = Modifier.border(
                                    width = if (event.id == selectedEventId) 2.dp else 0.dp,
                                    color = if (event.id == selectedEventId) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                AllEventCard(
                                    event = event,
                                    allEventsViewModel = allEventsViewModel,
                                    onCardClick = { eventId -> navController.navigate("singleEvent/$eventId") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}