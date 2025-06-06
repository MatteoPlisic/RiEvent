package com.example.rievent.ui.map

import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.ui.allevents.AllEventCard
import com.example.rievent.ui.utils.Drawer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsMapScreen(
    viewModel: MapViewModel = viewModel(),
    navController: NavController,
) {
    // Collect all necessary states from the ViewModel
    val allMapEvents by viewModel.allMapEvents.collectAsState() // Renamed for clarity
    val visibleEvents by viewModel.visibleEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedEventId by viewModel.selectedEventId.collectAsState()

    val primorjeGorskiKotarCenter = LatLng(45.3271, 14.4422)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(primorjeGorskiKotarCenter, 9f)
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // --- SYNCHRONIZATION LOGIC ---

    // 1. Update the visible events list when the map stops moving
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            // The map has settled, get its visible boundaries
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                viewModel.updateVisibleEvents(bounds)
            }
        } else {
            // User is moving the map, clear any specific event selection
            viewModel.clearEventSelection()
        }
    }

    // 2. Move the map when an event card is selected from the list
    LaunchedEffect(selectedEventId) {
        selectedEventId?.let { eventId ->
            val event = allMapEvents.find { it.id == eventId }
            event?.location?.let { geoPoint ->
                val position = LatLng(geoPoint.latitude, geoPoint.longitude)
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(position, 15f), // Zoom in closer
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
            // --- MAP COMPONENT ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Map takes the top half
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    allMapEvents.forEach { event ->
                        event.location?.let { geoPoint ->
                            Marker(
                                state = MarkerState(position = LatLng(geoPoint.latitude, geoPoint.longitude)),
                                title = event.name,
                                snippet = event.category,
                                onInfoWindowClick = {
                                    event.id?.let { navController.navigate("singleEvent/$it") }
                                },
                                // Optional: When marker is tapped, select it in the ViewModel
                                onClick = {
                                    viewModel.onEventCardSelected(event.id)
                                    false // Allow default behavior
                                }
                            )
                        }
                    }
                }
            }

            // --- LIST COMPONENT ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // List takes the bottom half
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (visibleEvents.isEmpty()) {
                    Text(
                        "No events found in this area.",
                        modifier = Modifier.padding(16.dp)
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
                                    allEventsViewModel = viewModel(),
                                    onCardClick = {eventId ->
                                        navController.navigate("singleEvent/$eventId") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}