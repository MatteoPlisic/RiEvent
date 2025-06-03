package com.example.rievent.ui.map // Your package

import Event // Import your Event data class
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MapMarkerInfo( // A simple data class to hold info for a map marker
    val id: String, // Event ID
    val position: GeoPoint,
    val title: String,
    val snippet: String?
)

class EventsMapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // This will hold events that have a GeoPoint location
    private val _eventsForMap = MutableStateFlow<List<Event>>(emptyList())
    val eventsForMap: StateFlow<List<Event>> = _eventsForMap.asStateFlow()



    init {
        fetchEventsWithLocations()
    }

    fun fetchEventsWithLocations() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // Query for events that have the 'location' field (and it's not null)
                // You might also want to filter by isPublic, upcoming events, etc.
                val result: QuerySnapshot = db.collection("Event")
                    .whereNotEqualTo("location", null) // Basic filter: location field must exist and not be null
                    // .whereEqualTo("isPublic", true) // Example: Only public events
                    // .orderBy("startTime", Query.Direction.ASCENDING) // Example: Order by start time
                    // .whereGreaterThanOrEqualTo("startTime", Timestamp.now()) // Example: Only upcoming events
                    .get()
                    .await()

                val events = result.toObjects(Event::class.java).filter {
                    // Double check location is not null, though whereNotEqualTo should handle it.
                    // Also, ensure the GeoPoint itself is valid if needed (e.g. not 0,0 if that's an invalid placeholder)
                    it.location != null
                }
                _eventsForMap.value = events
                Log.d("EventsMapVM", "Fetched ${events.size} events for map.")

            } catch (e: Exception) {
                Log.e("EventsMapVM", "Error fetching events for map", e)
                _errorMessage.value = "Failed to load event locations: ${e.message}"
                _eventsForMap.value = emptyList() // Clear events on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Later, we might add functions here like:
    // fun onMarkerClicked(eventId: String) { ... }
    // fun refreshEvents() { fetchEventsWithLocations() }
}