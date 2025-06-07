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
import androidx.compose.runtime.remember
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
    allEventsViewModel: AllEventsViewModel = viewModel(), // Still needed for AllEventCard
    navController: NavController,
) {
    // The UI now collects a single, comprehensive state object.
    val uiState by viewModel.uiState.collectAsState()

    val datePickerState = rememberDatePickerState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    if (uiState.isDatePickerDialogVisible) {
        DatePickerDialog(
            onDismissRequest = { viewModel.onDatePickerDialogToggled(false) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    } ?: viewModel.onDatePickerDialogToggled(false)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDatePickerDialogToggled(false) }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.3271, 14.4422), 12f)
    }
    val listState = rememberLazyListState()

    // The cluster items are now derived directly from the uiState
    val clusterItems = remember(uiState.allMapEvents) {
        uiState.allMapEvents.map { EventClusterItem(it) }
    }

    // Animate camera when an event is selected
    LaunchedEffect(uiState.selectedEventId) {
        uiState.selectedEventId?.let { eventId ->
            val event = uiState.allMapEvents.find { it.id == eventId }
            event?.location?.let { geoPoint ->
                val position = LatLng(geoPoint.latitude, geoPoint.longitude)
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, 15f), 1000)
            }
        }
    }

    // Update visible events when the camera stops moving
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            viewModel.onMapStartMoving()
        } else {
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                viewModel.onMapMoved(bounds)
            }
        }
    }

    Drawer(title = "Event Map", navController = navController, gesturesEnabled = false) { drawerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(drawerPadding)) {
            // Date Filter TextField
            OutlinedTextField(
                value = uiState.selectedDate?.format(dateFormatter) ?: "Any Date",
                onValueChange = {}, readOnly = true, label = { Text("Filter by Date") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { viewModel.onDatePickerDialogToggled(true) },
                trailingIcon = {
                    if (uiState.selectedDate != null) {
                        IconButton(onClick = { viewModel.onDateSelected(null) }) { Icon(Icons.Filled.Clear, "Clear Date Filter") }
                    } else {
                        IconButton(onClick = { viewModel.onDatePickerDialogToggled(true) }) { Icon(Icons.Filled.DateRange, "Select Date") }
                    }
                }
            )

            // Map Component
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    Clustering(
                        items = clusterItems,
                        onClusterItemInfoWindowClick = { item -> item.event.id?.let { navController.navigate("singleEvent/$it") } },
                        onClusterItemClick = { item -> viewModel.onEventCardSelected(item.event.id); false },
                    )
                }
            }

            // List Component
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.errorMessage != null) {
                    Text("Error: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
                } else if (uiState.visibleEvents.isEmpty()) {
                    Text(
                        if (uiState.selectedDate != null && uiState.allMapEvents.isEmpty()) "No events found for this date."
                        else "No events found in this map area."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(), state = listState,
                        contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.visibleEvents, key = { it.id!! }) { event ->
                            Box(
                                modifier = Modifier.border(
                                    width = if (event.id == uiState.selectedEventId) 2.dp else 0.dp,
                                    color = if (event.id == uiState.selectedEventId) MaterialTheme.colorScheme.primary else Color.Transparent
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