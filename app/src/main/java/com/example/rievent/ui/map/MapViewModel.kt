package com.example.rievent.ui.map

import Event
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

fun Timestamp.toLocalDate(): LocalDate {
    return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Single source of truth for the screen's state
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    // Internal state to hold the original, unfiltered list from Firestore
    private val _sourceEvents = MutableStateFlow<List<Event>>(emptyList())
    private val _dateFilter = MutableStateFlow<LocalDate?>(null)

    init {
        // When the ViewModel starts, fetch the master list of events
        fetchEventsWithLocations()

        // Set up a reactive pipeline that combines the source data and the date filter.
        // Whenever either one changes, it automatically re-calculates the `allMapEvents`
        // and updates the main UI state.
        viewModelScope.launch {
            combine(_sourceEvents, _dateFilter) { events, date ->
                if (date == null) {
                    events // No date filter, return all events
                } else {
                    events.filter { event ->
                        val eventStartDate = event.startTime?.toLocalDate() ?: return@filter false
                        val eventEndDate = event.endTime?.toLocalDate() ?: eventStartDate
                        !date.isBefore(eventStartDate) && !date.isAfter(eventEndDate)
                    }
                }
            }.collect { dateFilteredEvents ->
                // Update the main UI state with the newly filtered list.
                // We also reset the visible events to this new list initially.
                _uiState.update {
                    it.copy(
                        allMapEvents = dateFilteredEvents,
                        visibleEvents = dateFilteredEvents // Show all in the list by default
                    )
                }
            }
        }
    }

    private fun fetchEventsWithLocations() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = db.collection("Event")
                    .whereNotEqualTo("location", null)
                    .get().await()
                val events = result.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.apply { id = doc.id }
                }
                // Update the single source of truth. The `combine` flow will handle the rest.
                _sourceEvents.value = events
                Log.d("MapViewModel", "Fetched ${events.size} total events with locations.")
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching events for map", e)
                _uiState.update { it.copy(errorMessage = "Failed to load event locations.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- UI EVENT HANDLERS ---

    fun onDateSelected(date: LocalDate?) {
        _dateFilter.value = date // Update the date filter, the `combine` flow will react
        _uiState.update { it.copy(selectedDate = date, isDatePickerDialogVisible = false) }
    }

    fun onDatePickerDialogToggled(isVisible: Boolean) {
        _uiState.update { it.copy(isDatePickerDialogVisible = isVisible) }
    }

    fun onEventCardSelected(eventId: String?) {
        _uiState.update { it.copy(selectedEventId = eventId) }
    }

    fun onMapMoved(bounds: LatLngBounds?) {
        if (bounds == null) {
            // Reset visible events to all date-filtered events
            _uiState.update { it.copy(visibleEvents = it.allMapEvents) }
            return
        }
        val visible = _uiState.value.allMapEvents.filter { event ->
            event.location?.let {
                bounds.contains(LatLng(it.latitude, it.longitude))
            } ?: false
        }
        _uiState.update { it.copy(visibleEvents = visible) }
    }

    fun onMapStopMoving() {
        // This is where you might want to clear a selection if the selected
        // event is no longer visible, but for now we do nothing.
    }

    fun onMapStartMoving() {
        _uiState.update { it.copy(selectedEventId = null) }
    }
}