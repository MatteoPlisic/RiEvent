package com.example.rievent.ui.map

import Event
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // --- STATE FLOWS ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- NEW: Two separate lists for better state management ---
    // 1. Holds ALL events fetched from Firestore for the map
    val allMapEvents = MutableStateFlow<List<Event>>(emptyList())

    // 2. Holds only the events currently visible in the map's viewport
    private val _visibleEvents = MutableStateFlow<List<Event>>(emptyList())
    val visibleEvents: StateFlow<List<Event>> = _visibleEvents.asStateFlow()

    // Keep track of the currently selected event to highlight it
    private val _selectedEventId = MutableStateFlow<String?>(null)
    val selectedEventId: StateFlow<String?> = _selectedEventId.asStateFlow()

    init {
        fetchEventsWithLocations()
    }

    private fun fetchEventsWithLocations() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val result: QuerySnapshot = db.collection("Event")
                    .whereNotEqualTo("location", null)
                    .get()
                    .await()

                val events = result.documents.mapNotNull { doc ->
                    val event = doc.toObject(Event::class.java)
                    event?.apply { id = doc.id }
                }.filter { it.location != null }

                allMapEvents.value = events
                // Initially, all events are "visible" until the map provides bounds
                _visibleEvents.value = events
                Log.d("EventsMapVM", "Fetched ${events.size} total events for map.")

            } catch (e: Exception) {
                Log.e("EventsMapVM", "Error fetching events for map", e)
                _errorMessage.value = "Failed to load event locations: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filters the master list of events to only those within the map's current visible bounds.
     */
    fun updateVisibleEvents(bounds: LatLngBounds) {
        val visible = allMapEvents.value.filter { event ->
            event.location?.let {
                bounds.contains(LatLng(it.latitude, it.longitude))
            } ?: false
        }
        _visibleEvents.value = visible
        Log.d("EventsMapVM", "${visible.size} events are visible in the current viewport.")
    }

    /**
     * Sets the currently selected event ID, which can be used to move the map.
     */
    fun onEventCardSelected(eventId: String?) {
        _selectedEventId.value = eventId
    }

    /**
     * Clears the event selection, e.g., when the user starts moving the map again.
     */
    fun clearEventSelection() {
        _selectedEventId.value = null
    }
}