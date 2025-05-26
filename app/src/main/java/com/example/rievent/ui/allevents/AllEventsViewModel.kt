package com.example.rievent.ui.allevents

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import Event // Assuming Event data class is in the root package or correctly imported
import android.util.Log
// import androidx.compose.animation.core.copy // Unused import
import com.example.rievent.models.EventRSPV
import com.example.rievent.models.RsvpUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
// import com.google.firebase.firestore.toObject // toObject is an extension function
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // Map to store real-time RSVP data for each event: eventId -> EventRSPV?
    private val _eventsRsvpsMap = MutableStateFlow<Map<String, EventRSPV?>>(emptyMap())
    val eventsRsvpsMap = _eventsRsvpsMap.asStateFlow()

    private val allEvents = mutableListOf<Event>() // For local filtering
    private val rsvpListeners = mutableMapOf<String, ListenerRegistration>()
    private var eventsListenerRegistration: ListenerRegistration? = null

    // Store current search state to re-apply after allEvents list updates
    private var currentSearchQuery = ""
    private var currentSearchByUser = false
    private var currentSelectedCategory = "Any"

    init {
        // You might want to load events immediately, or rely on the Composable's LaunchedEffect
        // loadAllPublicEvents()
    }

    fun loadAllPublicEvents() {
        eventsListenerRegistration?.remove() // Remove previous listener if any
        eventsListenerRegistration = db.collection("Event")
            .whereEqualTo("public", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AllEventsViewModel", "Listen failed.", error)
                    // Optionally, update UI to show error state
                    // _errorMessage.value = "Error loading events: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w("AllEventsViewModel", "Snapshot was null")
                    return@addSnapshotListener
                }

                val freshEventsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val event = doc.toObject(Event::class.java)
                        event?.copy(id = doc.id) // Make sure Event.id is non-nullable if possible
                    } catch (e: Exception) {
                        Log.e("AllEventsViewModel", "Error deserializing event: ${doc.id}", e)
                        null
                    }
                }

                allEvents.clear()
                allEvents.addAll(freshEventsList)

                // Re-apply current search/filter to the updated list
                search(currentSearchQuery, currentSearchByUser, currentSelectedCategory)
            }
    }

    fun search(query: String, searchByUser: Boolean, category: String) {
        // Store current search parameters
        currentSearchQuery = query
        currentSearchByUser = searchByUser
        currentSelectedCategory = category

        val filtered = allEvents.filter { event ->
            val matchesText = if (searchByUser) {
                event.ownerName?.contains(query, ignoreCase = true) ?: false // Handle nullable ownerName
            } else {
                event.name.contains(query, ignoreCase = true)
            }
            val matchesCategory = category == "Any" ||
                    event.category.equals(category, ignoreCase = true)
            matchesText && matchesCategory
        }
        _events.value = filtered
    }

    fun listenToRsvpForEvent(eventId: String?) {
        if (eventId == null || eventId.isBlank() || rsvpListeners.containsKey(eventId)) {
            return // Don't listen if eventId is null/blank or already listening
        }

        val listener = db.collection("event_rspv")
            .whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AllEventsViewModel", "RSVP listen failed for $eventId.", error)
                    _eventsRsvpsMap.value = _eventsRsvpsMap.value.toMutableMap().apply {
                        put(eventId, null) // Indicate error or no data
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Assuming only one RSVP doc per eventId
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
                    // No RSVP document found for this eventId, or snapshot is empty
                    _eventsRsvpsMap.value = _eventsRsvpsMap.value.toMutableMap().apply {
                        // Represent no RSVP doc as null or a default EventRSPV if preferred
                        put(eventId, null)
                    }
                }
            }
        rsvpListeners[eventId] = listener
    }

    fun stopListeningToRsvp(eventId: String?) {
        if (eventId == null || eventId.isBlank()) return
        rsvpListeners.remove(eventId)?.remove()
        // Optionally, you can remove the entry from the map or leave it as is (stale data)
        // _eventsRsvpsMap.value = _eventsRsvpsMap.value.toMutableMap().apply { remove(eventId) }
    }

    fun updateRsvp(eventId: String?, newStatus: RsvpStatus) {
        if (eventId == null || eventId.isBlank()) {
            Log.e("RSVP_UPDATE", "Event ID is null or blank, cannot update RSVP.")
            return
        }
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("RSVP_UPDATE", "User is not logged in, cannot update RSVP.")
            // Potentially emit an error/message to UI
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

                    // Remove user from all lists first to handle status changes
                    updates["coming_users"] = FieldValue.arrayRemove(userRsvpProfile)
                    updates["maybe_users"] = FieldValue.arrayRemove(userRsvpProfile)
                    updates["not_coming_users"] = FieldValue.arrayRemove(userRsvpProfile)

                    // Add user to the new target list
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
                    // No RSVP document exists, create a new one
                    val newRsvpDoc = EventRSPV(
                        eventId = eventId, // Ensure EventRSPV has an eventId field
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