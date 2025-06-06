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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

fun Timestamp.toLocalDate(): LocalDate {
    return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // --- STATE FLOWS ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Holds the original, unfiltered list of all events with locations from Firestore
    private val _sourceEvents = MutableStateFlow<List<Event>>(emptyList())

    // State for the date filter
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    // This is the primary list for the UI. It's the result of filtering _sourceEvents by date.
    val allMapEvents: StateFlow<List<Event>> = combine(_sourceEvents, _selectedDate) { events, date ->
        if (date == null) {
            events // No date filter, return all events
        } else {
            events.filter { event ->
                // Date matching logic
                val eventStartDate = event.startTime?.toLocalDate()
                if (eventStartDate == null) return@filter false
                val eventEndDate = event.endTime?.toLocalDate() ?: eventStartDate
                !date.isBefore(eventStartDate) && !date.isAfter(eventEndDate)
            }
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())


    // Holds only the events currently visible in the map's viewport
    private val _visibleEvents = MutableStateFlow<List<Event>>(emptyList())
    val visibleEvents: StateFlow<List<Event>> = _visibleEvents.asStateFlow()

    // Keep track of the currently selected event to highlight it
    private val _selectedEventId = MutableStateFlow<String?>(null)
    val selectedEventId: StateFlow<String?> = _selectedEventId.asStateFlow()

    init {
        fetchEventsWithLocations()
    }

    fun fetchEventsWithLocations() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val result = db.collection("Event")
                    .whereNotEqualTo("location", null)
                    .get()
                    .await()

                val events = result.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.apply { id = doc.id }
                }

                // Update the single source of truth. The `combine` flow will do the rest.
                _sourceEvents.value = events
                updateVisibleEvents(null) // Refresh visible events with the new full list
                Log.d("MapViewModel", "Fetched ${events.size} total events with locations.")

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching events for map", e)
                _errorMessage.value = "Failed to load event locations: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filters the date-filtered list (`allMapEvents`) to only those within the map's current bounds.
     */
    fun updateVisibleEvents(bounds: LatLngBounds?) {
        val currentEvents = allMapEvents.value // This is already the date-filtered list
        val visible = if (bounds != null) {
            currentEvents.filter { event ->
                event.location?.let {
                    bounds.contains(LatLng(it.latitude, it.longitude))
                } ?: false
            }
        } else {
            currentEvents // On initial load or after date change, show all matching events in list
        }
        _visibleEvents.value = visible
    }

    /**
     * Public function for the UI to call when the user selects or clears a date.
     */
    fun onDateSelected(date: LocalDate?) {
        _selectedDate.value = date
        // Clear the spatial filter so the list below the map resets to show all events for the new date
        updateVisibleEvents(null)
    }

    fun onEventCardSelected(eventId: String?) {
        _selectedEventId.value = eventId
    }

    fun clearEventSelection() {
        _selectedEventId.value = null
    }
}