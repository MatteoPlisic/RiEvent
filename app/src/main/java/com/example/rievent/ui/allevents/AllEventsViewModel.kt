package com.example.rievent.ui.allevents

import Event
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.EventRSPV
import com.example.rievent.models.RsvpUser
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

fun Timestamp.toLocalDate(): LocalDate {
    return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

class AllEventsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(AllEventsUiState())
    val uiState = _uiState.asStateFlow()

    private var allEventsMasterList = listOf<Event>()
    private val rsvpListeners = mutableMapOf<String, ListenerRegistration>()
    private var eventsListenerRegistration: ListenerRegistration? = null

    private val _navigateToSingleEventAction = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val navigateToSingleEventAction: SharedFlow<String> = _navigateToSingleEventAction

    init {
        loadAllPublicEvents()
    }

    fun onSearchTextChanged(text: String) { _uiState.update { it.copy(searchText = text) }; applyFilters() }
    fun onSearchModeChanged(searchByUser: Boolean) { _uiState.update { it.copy(searchByUser = searchByUser) }; applyFilters() }
    fun onCategorySelected(category: String) { _uiState.update { it.copy(selectedCategory = category, isCategoryMenuExpanded = false) }; applyFilters() }
    fun onCategoryMenuToggled(isExpanded: Boolean) { _uiState.update { it.copy(isCategoryMenuExpanded = isExpanded) } }
    fun onDateSelected(date: LocalDate?) { _uiState.update { it.copy(selectedDate = date, isDatePickerDialogVisible = false) }; applyFilters() }
    fun onDatePickerDialogDismissed() { _uiState.update { it.copy(isDatePickerDialogVisible = false) } }
    fun onDatePickerDialogOpened() { _uiState.update { it.copy(isDatePickerDialogVisible = true) } }
    fun onDistanceChanged(distance: Float) { _uiState.update { it.copy(distanceFilterKm = distance) }; applyFilters() }
    fun onUserLocationUpdated(location: Location?) { _uiState.update { it.copy(userLocation = location) }; if (location != null) applyFilters() }
    fun onEventClicked(eventId: String?) { if (eventId.isNullOrBlank()) return; viewModelScope.launch { _navigateToSingleEventAction.emit(eventId) } }

    private fun loadAllPublicEvents() {
        _uiState.update { it.copy(isLoading = true) }
        eventsListenerRegistration?.remove()
        eventsListenerRegistration = db.collection("Event")
            .whereEqualTo("public", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AllEventsViewModel", "Listen failed.", error)
                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    Log.w("AllEventsViewModel", "Snapshot was null")
                    return@addSnapshotListener
                }
                val freshEventsList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }
                allEventsMasterList = freshEventsList
                applyFilters()
                _uiState.update { it.copy(isLoading = false) }
            }
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        val filtered = allEventsMasterList.filter { event ->
            val matchesText = if (currentState.searchByUser) {
                event.ownerName?.contains(currentState.searchText, ignoreCase = true) ?: false
            } else {
                event.name.contains(currentState.searchText, ignoreCase = true)
            }
            val matchesCategory = currentState.selectedCategory == "Any" || event.category.equals(currentState.selectedCategory, ignoreCase = true)
            val matchesDate = currentState.selectedDate == null || run {
                val eventStartDate = event.startTime?.toLocalDate() ?: return@run false
                val eventEndDate = event.endTime?.toLocalDate() ?: eventStartDate
                !currentState.selectedDate.isBefore(eventStartDate) && !currentState.selectedDate.isAfter(eventEndDate)
            }
            val matchesDistance = if (currentState.userLocation != null && event.location != null && currentState.distanceFilterKm < 50f) {
                calculateDistance(currentState.userLocation, event.location) <= currentState.distanceFilterKm
            } else { true }
            matchesText && matchesCategory && matchesDate && matchesDistance
        }
        val hasFilters = currentState.searchText.isNotEmpty() || currentState.selectedCategory != "Any" || currentState.selectedDate != null || currentState.distanceFilterKm < 50f

        // [THE FIX] - Update the listener map AFTER the filtering is done.
        updateRsvpListeners(filtered.mapNotNull { it.id })

        _uiState.update { it.copy(displayedEvents = filtered, hasAppliedFilters = hasFilters) }
    }

    private fun calculateDistance(userLocation: Location, eventGeoPoint: GeoPoint): Float {
        val eventLocation = Location("eventLocationProvider").apply {
            latitude = eventGeoPoint.latitude
            longitude = eventGeoPoint.longitude
        }
        return userLocation.distanceTo(eventLocation) / 1000f
    }


    private fun updateRsvpListeners(displayedEventIds: List<String>) {
        val currentListenerIds = rsvpListeners.keys.toSet()
        val newListenerIds = displayedEventIds.toSet()


        val idsToStop = currentListenerIds - newListenerIds
        idsToStop.forEach { eventId ->
            rsvpListeners.remove(eventId)?.remove()
            Log.d("AllEventsViewModel", "Stopped listening to RSVP for event: $eventId")
        }


        val idsToStart = newListenerIds - currentListenerIds
        idsToStart.forEach { eventId ->
            listenToRsvpForEvent(eventId)
        }
    }

    private fun listenToRsvpForEvent(eventId: String) {
        if (rsvpListeners.containsKey(eventId)) return // Already listening

        val listener = db.collection("event_rspv")
            .whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AllEventsViewModel", "RSVP listen failed for $eventId.", error)
                    return@addSnapshotListener
                }
                val rsvp = snapshot?.documents?.firstOrNull()?.toObject(EventRSPV::class.java)
                _uiState.update { currentState ->
                    val newMap = currentState.eventsRsvpsMap.toMutableMap()
                    newMap[eventId] = rsvp
                    currentState.copy(eventsRsvpsMap = newMap)
                }
            }
        rsvpListeners[eventId] = listener
        Log.d("AllEventsViewModel", "Started listening to RSVP for event: $eventId")
    }


    fun updateRsvp(eventId: String?, newStatus: RsvpStatus) {
        if (eventId.isNullOrBlank()) return
        val currentUser = auth.currentUser ?: return
        val userRsvpProfile = RsvpUser(currentUser.uid, currentUser.displayName ?: "Anonymous")

        db.collection("event_rspv").whereEqualTo("eventId", eventId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val docRef = snapshot.documents[0].reference
                    val updates = mutableMapOf<String, Any>(
                        "coming_users" to FieldValue.arrayRemove(userRsvpProfile),
                        "maybe_users" to FieldValue.arrayRemove(userRsvpProfile),
                        "not_coming_users" to FieldValue.arrayRemove(userRsvpProfile)
                    )
                    val targetField = when (newStatus) {
                        RsvpStatus.COMING -> "coming_users"
                        RsvpStatus.MAYBE -> "maybe_users"
                        RsvpStatus.NOT_COMING -> "not_coming_users"
                    }
                    updates[targetField] = FieldValue.arrayUnion(userRsvpProfile)
                    docRef.update(updates)
                } else {
                    val newRsvpDoc = EventRSPV(
                        eventId = eventId,
                        coming_users = if (newStatus == RsvpStatus.COMING) mutableListOf(userRsvpProfile) else mutableListOf(),
                        maybe_users = if (newStatus == RsvpStatus.MAYBE) mutableListOf(userRsvpProfile) else mutableListOf(),
                        not_coming_users = if (newStatus == RsvpStatus.NOT_COMING) mutableListOf(userRsvpProfile) else mutableListOf()
                    )
                    db.collection("event_rspv").add(newRsvpDoc)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        eventsListenerRegistration?.remove()
        rsvpListeners.values.forEach { it.remove() } // This correctly cleans up all remaining listeners
        rsvpListeners.clear()
        Log.d("AllEventsViewModel", "ViewModel cleared, all listeners removed.")
    }
}