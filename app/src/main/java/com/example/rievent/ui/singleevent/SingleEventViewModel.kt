package com.example.rievent.ui.singleevent // Or your actual package

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.EventRSPV // Adjust import if necessary
import com.example.rievent.models.RsvpUser // Adjust import
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import Event
import android.util.Log

import com.example.rievent.models.EventComment
import com.example.rievent.models.EventRating
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow


// import com.example.rievent.models.EventRating
// import com.example.rievent.models.EventComment





enum class RsvpStatus { COMING, MAYBE, NOT_COMING } // If not already globally available

class SingleEventViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _eventRsvp = MutableStateFlow<EventRSPV?>(null)
    val eventRsvp: StateFlow<EventRSPV?> = _eventRsvp.asStateFlow()

    private val _ratings = MutableStateFlow<List<EventRating>>(emptyList())
    val ratings: StateFlow<List<EventRating>> = _ratings.asStateFlow()

    private val _averageRating = MutableStateFlow(0.0f)
    val averageRating: StateFlow<Float> = _averageRating.asStateFlow()

    private val _ratingsSize = MutableStateFlow(0)
    val ratingsSize: StateFlow<Int> = _ratingsSize.asStateFlow()

    private val _userRating = MutableStateFlow<EventRating?>(null)
    val userRating: StateFlow<EventRating?> = _userRating.asStateFlow()

    private val _comments = MutableStateFlow<List<EventComment>>(emptyList())
    val comments: StateFlow<List<EventComment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var _enabledRatingButton = MutableStateFlow(true)
    val enabledRatingButton: StateFlow<Boolean> = _enabledRatingButton.asStateFlow()

    private var eventListener: ListenerRegistration? = null
    private var rsvpListener: ListenerRegistration? = null
    private var ratingsListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null

    fun loadEventData(eventId: String) {


        if (eventId.isBlank()) {
            _error.value = "Event ID is missing."
            return
        }
        _isLoading.value = true

        // Load Event Details
        eventListener?.remove()
        eventListener = db.collection("Event").document(eventId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("SingleEventVM", "Event listen failed.", e)
                    _error.value = "Failed to load event details: ${e.localizedMessage}"
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val loadedEvent = snapshot.toObject<Event>()?.copy(id = snapshot.id)
                    _event.value = loadedEvent
                } else {
                    _error.value = "Event not found."
                    _event.value = null // Explicitly nullify if not found
                }
                // _isLoading.value = false // Keep true until all initial data is loaded or defer
            }

        // Load RSVP
        rsvpListener?.remove()
        rsvpListener = db.collection("event_rspv").whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("SingleEventVM", "RSVP listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    _eventRsvp.value = snapshot.documents[0].toObject<EventRSPV>()
                } else {
                    // No RSVP doc yet, create a default one for display or handle as null
                    _eventRsvp.value = EventRSPV(eventId = eventId) // Or null if you prefer to show "no data"
                }
            }

        // Load Ratings
        ratingsListener?.remove()
        ratingsListener = db.collection("event_ratings").whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("SingleEventVM", "Ratings listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val currentRatings = snapshot.documents.mapNotNull { doc ->
                        doc.toObject<EventRating>()?.copy(id = doc.id) // Manually set the ID
                    }
                    _ratings.value = currentRatings

                    calculateAverageRating(currentRatings)
                    _ratingsSize.value = currentRatings.size
                    if(_event.value?.endTime != null)
                            _enabledRatingButton.value = Timestamp.now() > _event.value!!.endTime!!
                    Log.d("enabled rating", _enabledRatingButton.value.toString())
                    _userRating.value = currentRatings.find { it.userId == currentUserId && it.eventId == eventId }
                }
            }


        commentsListener?.remove()
        commentsListener = db.collection("event_comments").whereEqualTo("eventId", eventId)
            .orderBy("createdAt", Query.Direction.DESCENDING) // Show newest comments first
            .limit(50) // Limit comments for performance
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("SingleEventVM", "Comments listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _comments.value = snapshot.toObjects<EventComment>()
                }
                _isLoading.value = false // Set loading to false after all listeners are attached
                return@addSnapshotListener
            }
    }

    private fun calculateAverageRating(ratingsList: List<EventRating>) {
        if (ratingsList.isEmpty()) {
            _averageRating.value = 0.0f
        } else {
            _averageRating.value = ratingsList.map { it.rating }.average().toFloat()
        }
    }

    fun updateRsvp(eventId: String, newStatus: RsvpStatus) {
        val uid = currentUserId ?: return Unit.also { _error.value = "User not logged in." }
        val userName = auth.currentUser?.displayName ?: "Anonymous"
        val userRsvpProfile = RsvpUser(uid, userName)

        db.collection("event_rspv").whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                val docRef = if (!snapshot.isEmpty) {
                    snapshot.documents[0].reference
                } else {
                    // Create new RSVP doc if it doesn't exist
                    val newRsvpDoc = EventRSPV(eventId = eventId)
                    val newDocRef = db.collection("event_rspv").document() // Auto-generate ID
                    newDocRef.set(newRsvpDoc) // Set initial empty doc
                    newDocRef // return the new doc ref
                }

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
                    .addOnFailureListener { e -> _error.value = "RSVP update failed: ${e.localizedMessage}" }
            }
            .addOnFailureListener { e -> _error.value = "Failed to fetch RSVP doc for update: ${e.localizedMessage}" }
    }

    fun submitRating(eventId: String, ratingValue: Float) {
        val uid = currentUserId ?: return Unit.also { _error.value = "User not logged in." }
        val userName = auth.currentUser?.displayName ?: "Anonymous"

        // Check if user has already rated. If so, update; otherwise, create new.
        val existingRating = _ratings.value.firstOrNull { it.userId == uid && it.eventId == eventId }

        if (existingRating != null && existingRating.id != null) {
            // Update existing rating
            db.collection("event_ratings").document(existingRating.id)
                .update("rating", ratingValue, "createdAt", Timestamp.now())
                .addOnFailureListener { e -> _error.value = "Failed to update rating: ${e.localizedMessage}" }
        } else {
            // Add new rating
            val newRating = EventRating(
                eventId = eventId,
                userId = uid,
                userName = userName,
                rating = ratingValue
            )
            db.collection("event_ratings").add(newRating)
                .addOnFailureListener { e -> _error.value = "Failed to submit rating: ${e.localizedMessage}" }
        }
    }

    fun addComment(eventId: String, commentText: String) {
        val uid = currentUserId ?: return Unit.also { _error.value = "User not logged in." }
        if (commentText.isBlank()) {
            _error.value = "Comment cannot be empty."
            return
        }
        val userName = auth.currentUser?.displayName ?: "Anonymous"
        val profilePic = auth.currentUser?.photoUrl?.toString()

        val newComment = EventComment(
            eventId = eventId,
            userId = uid,
            userName = userName,
            commentText = commentText,
            profileImageUrl = profilePic
        )
        db.collection("event_comments").add(newComment)
            .addOnSuccessListener { /* Optionally clear comment input field via a callback or state */ }
            .addOnFailureListener { e -> _error.value = "Failed to add comment: ${e.localizedMessage}" }
    }

    override fun onCleared() {
        super.onCleared()
        eventListener?.remove()
        rsvpListener?.remove()
        ratingsListener?.remove()
        commentsListener?.remove()
        Log.d("SingleEventVM", "ViewModel cleared, listeners removed.")
    }
}