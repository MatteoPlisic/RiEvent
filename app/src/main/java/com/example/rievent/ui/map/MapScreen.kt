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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.R
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
                }) { Text(stringResource(id = R.string.dialog_submit_button)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDatePickerDialogToggled(false) }) { Text(stringResource(id = R.string.dialog_cancel_button)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.3271, 14.4422), 12f)
    }
    val listState = rememberLazyListState()
    val clusterItems = remember(uiState.allMapEvents) {
        uiState.allMapEvents.map { EventClusterItem(it) }
    }

    LaunchedEffect(uiState.selectedEventId) {
        uiState.selectedEventId?.let { eventId ->
            val event = uiState.allMapEvents.find { it.id == eventId }
            event?.location?.let { geoPoint ->
                val position = LatLng(geoPoint.latitude, geoPoint.longitude)
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, 15f), 1000)
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            viewModel.onMapStartMoving()
        } else {
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                viewModel.onMapMoved(bounds)
            }
        }
    }

    Drawer(title = stringResource(id = R.string.map_screen_title), navController = navController, gesturesEnabled = false) { drawerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(drawerPadding)) {
            OutlinedTextField(
                value = uiState.selectedDate?.format(dateFormatter) ?: stringResource(id = R.string.map_screen_any_date),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(id = R.string.map_screen_filter_by_date)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { viewModel.onDatePickerDialogToggled(true) },
                trailingIcon = {
                    if (uiState.selectedDate != null) {
                        IconButton(onClick = { viewModel.onDateSelected(null) }) { Icon(Icons.Filled.Clear, stringResource(id = R.string.map_screen_clear_date_filter)) }
                    } else {
                        IconButton(onClick = { viewModel.onDatePickerDialogToggled(true) }) { Icon(Icons.Filled.DateRange, stringResource(id = R.string.map_screen_select_date)) }
                    }
                }
            )

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

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.errorMessage != null) {
                    Text(stringResource(id = R.string.generic_error_prefix, uiState.errorMessage!!), color = MaterialTheme.colorScheme.error)
                } else if (uiState.visibleEvents.isEmpty()) {
                    Text(
                        if (uiState.selectedDate != null && uiState.allMapEvents.isEmpty()) stringResource(id = R.string.map_screen_no_events_for_date)
                        else stringResource(id = R.string.map_screen_no_events_in_area)
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