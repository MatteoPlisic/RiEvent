package com.example.rievent.ui.map // Ensure this is the correct package for your map screen

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.ui.utils.Drawer // Assuming you use your Drawer here
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// ViewModel for this screen
// import com.example.rievent.ui.map.EventsMapViewModel // Already in the package


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsMapScreen(
    // ViewModel is instantiated here. If it had constructor args, you'd use a factory.
    viewModel: EventsMapViewModel = viewModel(),
    navController: NavController,
) {
    val eventsForMap by viewModel.eventsForMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Initial camera position (e.g., centered on Primorje-Gorski Kotar County if that's the default focus)
    // You'll need to adjust these coordinates and zoom.
    // These are very rough coordinates for PGŽ - Rijeka area.
    val primorjeGorskiKotarCenter = LatLng(45.3271, 14.4422) // Rijeka as an example center
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(primorjeGorskiKotarCenter, 9f) // Zoom level 9f is more regional
    }

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false, // Requires location permission and setup
                mapToolbarEnabled = true
            )
        )
    }

    val properties by remember {
        mutableStateOf(MapProperties(isMyLocationEnabled = false)) // Requires location permission
    }

    Drawer(
        // Using your Drawer composable
        title = "Event Map",
        navController = navController,
        { drawerPadding -> // Assuming your Drawer provides padding for its content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(drawerPadding) // Apply padding from Drawer
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = properties,
                    uiSettings = uiSettings,
                    onMapLoaded = {
                        Log.d("EventsMapScreen", "Map has loaded.")
                        // Consider moving camera to fit markers if events are already loaded
                        // and if you have multiple markers.
                    },
                    onPOIClick = { poi ->
                        Log.d("EventsMapScreen", "POI Clicked: ${poi.name}")
                    }
                ) {
                    eventsForMap.forEach { event ->
                        event.location?.let { geoPoint ->
                            // Ensure event.id is not null, provide a fallback or handle error
                            val eventId = event.id ?: "unknown_event_${event.name}"
                            Marker(
                                state = MarkerState(
                                    position = LatLng(
                                        geoPoint.latitude,
                                        geoPoint.longitude
                                    )
                                ),
                                title = event.name,
                                snippet = event.category, // Or a short description
                                onInfoWindowClick = {
                                    Log.d(
                                        "EventsMapScreen",
                                        "Info window clicked for event ID: $eventId"
                                    )
                                    navController.navigate("singleEvent/$eventId")
                                },
                                onClick = { marker ->
                                    Log.d("EventsMapScreen", "Marker clicked: ${event.name}")
                                    // marker.showInfoWindow() // Default behavior usually shows it.
                                    false // Return false to allow default behavior (show info window)
                                }
                            )
                        }
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), // Padding for the error message box
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error loading map data: $error", // More specific error context
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp) // Inner padding for text
                        )
                    }
                }
            }
        },

    )
    // Add onNavigateToMap to your Drawer's parameters if it has a map item
    // For example, if your Drawer's content lambda defines menu items, one could call onNavigateToMap

}