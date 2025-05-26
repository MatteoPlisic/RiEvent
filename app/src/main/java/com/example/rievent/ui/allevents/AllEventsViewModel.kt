package com.example.rievent.ui.allevents

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import Event
import android.util.Log
import androidx.compose.animation.core.copy
import com.example.rievent.models.EventRSPV
import com.example.rievent.models.RsvpUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


enum class RsvpStatus {
    COMING,
    MAYBE,
    NOT_COMING
}

class AllEventsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()
    val eventsRSPV = MutableStateFlow<List<EventRSPV>>(emptyList())
    private val allEvents = mutableListOf<Event>()

    fun loadAllPublicEvents() {
        db.collection("Event")
            .whereEqualTo("public", true)
            .addSnapshotListener { snapshot,error ->

                if (error != null) {
                    Log.w("AllEventsViewModel", "Listen failed.", error)
                    // Optionally, update UI to show error state
                    // _errorMessage.value = "Error loading events: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                // Check if the snapshot is null (shouldn't happen if error is null, but good practice)
                if (snapshot == null) {
                    Log.w("AllEventsViewModel", "Snapshot was null")
                    return@addSnapshotListener
                }

                // Map the documents to your Event objects
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val event = doc.toObject(Event::class.java)
                        // Important: Set the document ID on your Event object
                        event?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("AllEventsViewModel", "Error deserializing event: ${doc.id}", e)
                        null // Skip this document if deserialization fails
                    }
                }

                // Update your StateFlow with the new list
                // This will trigger UI recomposition if observed
                _events.value = list

                // If you were using allEvents for filtering, you'd update it here too.
                // allEvents.clear()
                // allEvents.addAll(list)

    }
    }

    fun search(query: String, searchByUser: Boolean, category: String) {
        val filtered = allEvents.filter { event ->
            val matchesText = if (searchByUser) {
                event.ownerName
                    .contains(query, ignoreCase = true)
            } else {
                event.name.contains(query, ignoreCase = true)
            }
            val matchesCategory = category == "Any" ||
                    event.category.equals(category, ignoreCase = true)
            matchesText && matchesCategory
        }
        _events.value = filtered

    }

    fun rsvp(
        id: String?,
        onComplete: (List<EventRSPV>) -> Unit
    ) {
        db.collection("event_rspv")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents
                    .mapNotNull {doc ->
                        val eventRSPV = doc.toObject(EventRSPV::class.java)
                        eventRSPV?.copy(eventId = doc.getString("eventId") ?: "")
                    }
                eventsRSPV.value = list
                onComplete(list)
            }
            .addOnFailureListener {
                onComplete(emptyList()) // or handle error differently
            }
    }

    fun updateRsvp(eventId: String?, coming: RsvpStatus) {
        db.collection("event_rspv")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val docRef = snapshot.documents[0].reference

                    // Pick the array field based on the enum
                    val fieldName = when (coming) {
                        RsvpStatus.COMING     -> "coming_users"
                        RsvpStatus.MAYBE      -> "maybe_users"
                        RsvpStatus.NOT_COMING -> "not_coming_users"
                    }
                    val user = RsvpUser(FirebaseAuth.getInstance().currentUser?.uid ?: "",FirebaseAuth.getInstance().currentUser?.displayName?:"")
                    // Atomically add the user object to that array
                    docRef.update(
                        fieldName,
                        FieldValue.arrayUnion(user)
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("RSVP", "Failed to update RSVP", e)
            }

    }
}
