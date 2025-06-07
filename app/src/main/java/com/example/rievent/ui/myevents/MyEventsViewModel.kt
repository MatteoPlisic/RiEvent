package com.example.rievent.ui.myevents

import Event
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyEventsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private var eventsListener: ListenerRegistration? = null

    // Single source of truth for the screen's state
    private val _uiState = MutableStateFlow(MyEventsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadEvents(ownerId: String) {
        _uiState.update { it.copy(isLoading = true) }
        eventsListener?.remove()
        eventsListener = db.collection("Event")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // In a real app, you might want to show an error message in the UI
                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                val eventList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _uiState.update { it.copy(myEvents = eventList, isLoading = false) }
            }
    }

    // --- DIALOG AND DELETION LOGIC ---

    /**
     * Called when the user clicks the delete button on an event card.
     * Updates the state to show the confirmation dialog.
     */
    fun onDeletionInitiated(event: Event) {
        _uiState.update { it.copy(eventToDelete = event) }
    }

    /**
     * Called when the user dismisses the dialog (clicks Cancel or outside the dialog).
     * Updates the state to hide the dialog.
     */
    fun onDeletionDismissed() {
        _uiState.update { it.copy(eventToDelete = null) }
    }

    /**
     * Called when the user confirms the deletion.
     * It deletes the event and its associated RSVP document, then hides the dialog.
     */
    fun onDeletionConfirmed() {
        val event = _uiState.value.eventToDelete ?: return

        viewModelScope.launch {
            try {
                // Delete the main event document
                db.collection("Event").document(event.id!!).delete().await()

                // Find and delete the associated event_rspv document
                val rsvpQuery = db.collection("event_rspv")
                    .whereEqualTo("eventId", event.id)
                    .limit(1)
                    .get().await()

                if (!rsvpQuery.isEmpty) {
                    rsvpQuery.documents[0].reference.delete().await()
                }

            } catch (e: Exception) {
                // Handle deletion error, e.g., show a snackbar message
            } finally {
                // Hide the dialog regardless of success or failure
                _uiState.update { it.copy(eventToDelete = null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventsListener?.remove()
    }
}