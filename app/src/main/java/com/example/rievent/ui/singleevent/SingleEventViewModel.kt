package com.example.rievent.ui.singleevent

import Event
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.EventComment
import com.example.rievent.models.EventRSPV
import com.example.rievent.models.EventRating
import com.example.rievent.models.RsvpUser
import com.example.rievent.ui.allevents.RsvpStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SingleEventViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    // Single source of truth for the screen's state
    private val _uiState = MutableStateFlow(SingleEventUiState())
    val uiState = _uiState.asStateFlow()

    // Internal state to hold the full ratings objects for logic
    private var allRatings: List<EventRating> = emptyList()

    private var eventListener: ListenerRegistration? = null
    private var rsvpListener: ListenerRegistration? = null
    private var ratingsListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null

    fun loadEventData(eventId: String) {
        if (eventId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Event ID is missing.", isLoading = false) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }

        attachEventListeners(eventId)
    }

    private fun attachEventListeners(eventId: String) {
        val uid = currentUserId

        // Load Event Details
        eventListener?.remove()
        eventListener = db.collection("Event").document(eventId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    _uiState.update { it.copy(errorMessage = "Event not found.", event = null, isLoading = false) }
                    return@addSnapshotListener
                }
                val loadedEvent = snapshot.toObject<Event>()?.copy(id = snapshot.id)
                val isRatingEnabled = loadedEvent?.endTime?.let { Timestamp.now() > it } ?: false
                _uiState.update { it.copy(event = loadedEvent, isRatingEnabled = isRatingEnabled) }
            }

        // Load RSVP
        rsvpListener?.remove()
        rsvpListener = db.collection("event_rspv").whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, _ ->
                val rsvp = snapshot?.documents?.firstOrNull()?.toObject<EventRSPV>() ?: EventRSPV(eventId = eventId)
                _uiState.update { it.copy(rsvp = rsvp) }
            }

        // Load Ratings
        ratingsListener?.remove()
        ratingsListener = db.collection("event_ratings").whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, _ ->
                val ratingsList = snapshot?.toObjects<EventRating>() ?: emptyList()
                allRatings = ratingsList // Store the full list internally

                val avg = if (ratingsList.isEmpty()) 0.0f else ratingsList.map { it.rating }.average().toFloat()
                val userRatingValue = ratingsList.find { it.userId == uid }?.rating

                _uiState.update { it.copy(averageRating = avg, totalRatings = ratingsList.size, userRating = userRatingValue) }
            }

        // Load Comments
        commentsListener?.remove()
        commentsListener = db.collection("event_comments").whereEqualTo("eventId", eventId)
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { snapshot, _ ->
                val commentsList = snapshot?.toObjects<EventComment>() ?: emptyList()
                _uiState.update { it.copy(comments = commentsList, isLoading = false) } // Final load, set loading to false
            }
    }

    // --- UI EVENT HANDLERS ---

    fun onNewCommentChange(text: String) {
        _uiState.update { it.copy(newCommentText = text) }
    }

    fun onRatingDialogToggled(isVisible: Boolean) {
        _uiState.update { it.copy(isRatingDialogVisible = isVisible) }
    }

    fun addComment(eventId: String) {
        val uid = currentUserId ?: return
        val commentText = _uiState.value.newCommentText.trim()
        if (commentText.isBlank()) return

        viewModelScope.launch {
            try {
                // Step 1: Determine the user's name.
                var userName = auth.currentUser?.displayName
                // If the name is null or blank, fetch it from Firestore.
                if (userName.isNullOrBlank()) {
                    val userDoc = db.collection("users").document(uid).get().await()
                    userName = userDoc.getString("displayName") ?: "Anonymous" // Fallback to Anonymous
                }

                // Step 2: Get the profile picture URL.
                val profilePic = auth.currentUser?.photoUrl?.toString()

                // Step 3: Create the comment object with the definitive name.
                val newComment = EventComment(
                    eventId = eventId,
                    userId = uid,
                    userName = userName,
                    commentText = commentText,
                    profileImageUrl = profilePic
                )

                // Step 4: Add the comment to Firestore.
                db.collection("event_comments").add(newComment).await()

                // Step 5: Clear the input field in the UI on success.
                _uiState.update { it.copy(newCommentText = "") }

            } catch (e: Exception) {
                Log.e("SingleEventVM", "Failed to add comment", e)
                _uiState.update { it.copy(errorMessage = "Failed to add comment.") }
            }
        }
    }

    fun submitRating(eventId: String, ratingValue: Float) {
        val uid = currentUserId ?: return
        onRatingDialogToggled(false) // Hide dialog immediately

        val existingRating = allRatings.firstOrNull { it.userId == uid }

        if (existingRating != null && existingRating.id != null) {
            db.collection("event_ratings").document(existingRating.id)
                .update("rating", ratingValue, "createdAt", Timestamp.now())
        } else {
            val newRating = EventRating(
                eventId = eventId,
                userId = uid,
                userName = auth.currentUser?.displayName ?: "Anonymous",
                rating = ratingValue
            )
            db.collection("event_ratings").add(newRating)
        }
    }

    fun updateRsvp(eventId: String, newStatus: RsvpStatus) {
        val uid = currentUserId ?: return
        val userRsvpProfile = RsvpUser(uid, auth.currentUser?.displayName ?: "Anonymous")

        db.collection("event_rspv").whereEqualTo("eventId", eventId).get()
            .addOnSuccessListener { snapshot ->
                val docRef = snapshot.documents.firstOrNull()?.reference
                    ?: db.collection("event_rspv").document().also { it.set(EventRSPV(eventId = eventId)) }

                val updates = mapOf(
                    "coming_users" to FieldValue.arrayRemove(userRsvpProfile),
                    "maybe_users" to FieldValue.arrayRemove(userRsvpProfile),
                    "not_coming_users" to FieldValue.arrayRemove(userRsvpProfile)
                )
                docRef.update(updates).addOnSuccessListener {
                    val targetField = when (newStatus) {
                        RsvpStatus.COMING -> "coming_users"
                        RsvpStatus.MAYBE -> "maybe_users"
                        RsvpStatus.NOT_COMING -> "not_coming_users"
                    }
                    docRef.update(targetField, FieldValue.arrayUnion(userRsvpProfile))
                }
            }
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