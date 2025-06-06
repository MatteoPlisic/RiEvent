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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

fun Timestamp.toLocalDate(): LocalDate {
    return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}
enum class RsvpStatus {
    COMING,
    MAYBE,
    NOT_COMING
}

class AllEventsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()

    private val _eventsRsvpsMap = MutableStateFlow<Map<String, EventRSPV?>>(emptyMap())
    val eventsRsvpsMap = _eventsRsvpsMap.asStateFlow()

    private val allEvents = mutableListOf<Event>()
    private val rsvpListeners = mutableMapOf<String, ListenerRegistration>()
    private var eventsListenerRegistration: ListenerRegistration? = null

    // State holders for current filter values
    private var currentSearchQuery = ""
    private var currentSearchByUser = false
    private var currentSelectedCategory = "Any"
    private var currentSelectedDate: LocalDate? = null
    private var currentDistanceKm: Float = 50f
    private var currentUserLocation: Location? = null

    private val _navigateToSingleEventAction = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val navigateToSingleEventAction: SharedFlow<String> = _navigateToSingleEventAction

    fun loadAllPublicEvents() {
        eventsListenerRegistration?.remove()
        eventsListenerRegistration = db.collection("Event")
            .whereEqualTo("public", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AllEventsViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w("AllEventsViewModel", "Snapshot was null")
                    return@addSnapshotListener
                }

                val freshEventsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val event = doc.toObject(Event::class.java)
                        event?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("AllEventsViewModel", "Error deserializing event: ${doc.id}", e)
                        null
                    }
                }

                allEvents.clear()
                allEvents.addAll(freshEventsList)
                // Re-apply current filters whenever data is refreshed
                search(
                    currentSearchQuery,
                    currentSearchByUser,
                    currentSelectedCategory,
                    currentSelectedDate,
                    currentDistanceKm,
                    currentUserLocation
                )
            }
    }

    fun search(
        query: String,
        searchByUser: Boolean,
        category: String,
        date: LocalDate?,
        distanceKm: Float,
        userLocation: Location?
    ) {
        // Store current filter values
        currentSearchQuery = query
        currentSearchByUser = searchByUser
        currentSelectedCategory = category
        currentSelectedDate = date
        currentDistanceKm = distanceKm
        currentUserLocation = userLocation

        val filtered = allEvents.filter { event ->
            val matchesText = if (searchByUser) {
                event.ownerName?.contains(query, ignoreCase = true) ?: false
            } else {
                event.name.contains(query, ignoreCase = true)
            }

            val matchesCategory = category == "Any" ||
                    event.category.equals(category, ignoreCase = true)

            val matchesDate = date == null || run {
                val eventStartDate = event.startTime?.toLocalDate()
                if (eventStartDate == null) return@run false
                val eventEndDate = event.endTime?.toLocalDate() ?: eventStartDate
                !date.isBefore(eventStartDate) && !date.isAfter(eventEndDate)
            }

            val matchesDistance = if (userLocation != null && event.location != null) {
                // Only filter by distance if both user and event locations are available
                calculateDistance(userLocation, event.location) <= distanceKm
            } else {
                // If either location is missing, this filter passes
                true
            }

            matchesText && matchesCategory && matchesDate && matchesDistance
        }
        _events.value = filtered
    }

    private fun calculateDistance(userLocation: Location, eventGeoPoint: GeoPoint): Float {
        val eventLocation = Location("eventLocationProvider").apply {
            latitude = eventGeoPoint.latitude
            longitude = eventGeoPoint.longitude
        }
        // distanceTo returns distance in meters, so we convert to kilometers
        return userLocation.distanceTo(eventLocation) / 1000f
    }

    fun listenToRsvpForEvent(eventId: String?) {
        if (eventId == null || eventId.isBlank() || rsvpListeners.containsKey(eventId)) {
            return
        }

        val listener = db.collection("event_rspv")
            .whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AllEventsViewModel", "RSVP listen failed for $eventId.", error)
                    _eventsRsvpsMap.value = _eventsRsvpsMap.value.toMutableMap().apply {
                        put(eventId, null)
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    try {
                        val eventRSPV = doc.toObject(EventRSPV::class.java)
                        _eventsRsvpsMap.value = _eventsRsvpsMap.value.toMutableMap().apply {
                            put(eventId, eventRSPV)
                        }
                    } catch (e: Exception) {
                        Log.e("AllEventsViewModel", "Error deserializing RSVP for $eventId: ${doc.id}", e)
                    }
                } else {
                    _eventsRsvpsMap.value = _eventsRsvpsMap.value.toMutableMap().apply {
                        put(eventId, null)
                    }
                }
            }
        rsvpListeners[eventId] = listener
    }

    fun stopListeningToRsvp(eventId: String?) {
        if (eventId == null || eventId.isBlank()) return
        rsvpListeners.remove(eventId)?.remove()
    }

    fun updateRsvp(eventId: String?, newStatus: RsvpStatus) {
        if (eventId.isNullOrBlank()) return
        val currentUser = auth.currentUser ?: return
        val userRsvpProfile = RsvpUser(currentUser.uid, currentUser.displayName ?: "Anonymous")

        db.collection("event_rspv")
            .whereEqualTo("eventId", eventId)
            .get()
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
        rsvpListeners.values.forEach { it.remove() }
        rsvpListeners.clear()
        Log.d("AllEventsViewModel", "ViewModel cleared, listeners removed.")
    }

    fun onEventClicked(eventId: String?) {
        if (eventId.isNullOrBlank()) return
        viewModelScope.launch {
            _navigateToSingleEventAction.emit(eventId)
        }
    }
}