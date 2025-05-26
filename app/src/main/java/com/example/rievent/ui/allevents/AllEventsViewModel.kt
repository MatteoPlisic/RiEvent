package com.example.rievent.ui.allevents

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import Event // Assuming Event data class is in the root package or correctly imported
import android.util.Log
import com.example.rievent.models.EventRSPV
import com.example.rievent.models.RsvpUser
// Import the extension function if it's in a different package

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.Timestamp
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

    private var currentSearchQuery = ""
    private var currentSearchByUser = false
    private var currentSelectedCategory = "Any"
    private var currentSelectedDate: LocalDate? = null // Added for date filter state

    init {
        // loadAllPublicEvents() // Rely on Composable's LaunchedEffect
    }

    fun loadAllPublicEvents() {
        eventsListenerRegistration?.remove()
        eventsListenerRegistration = db.collection("Event")
            .whereEqualTo("public", true) // Assuming your field is "public" not "isPublic" in Firestore
            // If your field in Firestore is "isPublic", use: .whereEqualTo("isPublic", true)
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
                search(currentSearchQuery, currentSearchByUser, currentSelectedCategory, currentSelectedDate) // Pass currentSelectedDate
            }
    }

    fun search(query: String, searchByUser: Boolean, category: String, date: LocalDate?) { // Added date parameter
        currentSearchQuery = query
        currentSearchByUser = searchByUser
        currentSelectedCategory = category
        currentSelectedDate = date // Store current selected date

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
                if (eventStartDate == null) return@run false // Event must have a start time to be matched by date

                // If endTime is null, event is considered for that single start date
                val eventEndDate = event.endTime?.toLocalDate() ?: eventStartDate

                // Check if the selected 'date' is within the event's start and end dates (inclusive)
                !date.isBefore(eventStartDate) && !date.isAfter(eventEndDate)
            }

            matchesText && matchesCategory && matchesDate // Added matchesDate
        }
        _events.value = filtered
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
                        _eventsRsvpsMap.value = _eventsRsvpsMap.value.toMutableMap().apply {
                            put(eventId, null)
                        }
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
        if (eventId == null || eventId.isBlank()) {
            Log.e("RSVP_UPDATE", "Event ID is null or blank, cannot update RSVP.")
            return
        }
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("RSVP_UPDATE", "User is not logged in, cannot update RSVP.")
            return
        }
        val userRsvpProfile = RsvpUser(currentUser.uid, currentUser.displayName ?: "Anonymous")

        db.collection("event_rspv")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val docRef = snapshot.documents[0].reference
                    val updates = mutableMapOf<String, Any>()

                    updates["coming_users"] = FieldValue.arrayRemove(userRsvpProfile)
                    updates["maybe_users"] = FieldValue.arrayRemove(userRsvpProfile)
                    updates["not_coming_users"] = FieldValue.arrayRemove(userRsvpProfile)

                    val targetField = when (newStatus) {
                        RsvpStatus.COMING -> "coming_users"
                        RsvpStatus.MAYBE -> "maybe_users"
                        RsvpStatus.NOT_COMING -> "not_coming_users"
                    }
                    updates[targetField] = FieldValue.arrayUnion(userRsvpProfile)

                    docRef.update(updates)
                        .addOnSuccessListener { Log.d("RSVP_UPDATE", "RSVP updated successfully for $eventId to $newStatus") }
                        .addOnFailureListener { e -> Log.e("RSVP_UPDATE", "Failed to update RSVP for $eventId", e) }
                } else {
                    val newRsvpDoc = EventRSPV(
                        eventId = eventId,
                        coming_users = if (newStatus == RsvpStatus.COMING) mutableListOf(userRsvpProfile) else mutableListOf(),
                        maybe_users = if (newStatus == RsvpStatus.MAYBE) mutableListOf(userRsvpProfile) else mutableListOf(),
                        not_coming_users = if (newStatus == RsvpStatus.NOT_COMING) mutableListOf(userRsvpProfile) else mutableListOf()
                    )
                    db.collection("event_rspv").add(newRsvpDoc)
                        .addOnSuccessListener { docRef -> Log.d("RSVP_UPDATE", "New RSVP document created (${docRef.id}) and RSVP set for $eventId to $newStatus") }
                        .addOnFailureListener { e -> Log.e("RSVP_UPDATE", "Failed to create new RSVP document for $eventId", e) }
                }
            }
            .addOnFailureListener { e ->
                Log.e("RSVP_UPDATE", "Failed to fetch RSVP document for update for $eventId", e)
            }
    }

    override fun onCleared() {
        super.onCleared()
        eventsListenerRegistration?.remove()
        rsvpListeners.values.forEach { it.remove() }
        rsvpListeners.clear()
        Log.d("AllEventsViewModel", "ViewModel cleared, listeners removed.")
    }
}