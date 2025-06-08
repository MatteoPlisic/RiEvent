package com.example.rievent.ui.myevents

import Event
import android.os.SystemClock.sleep
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


    private val _uiState = MutableStateFlow(MyEventsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadEvents(ownerId: String) {
        _uiState.update { it.copy(isLoading = true) }
        eventsListener?.remove()
        eventsListener = db.collection("Event")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {

                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                val eventList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _uiState.update { it.copy(myEvents = eventList, isLoading = false) }
            }
    }


    fun onDeletionInitiated(event: Event) {
        _uiState.update { it.copy(eventToDelete = event) }
    }


    fun onDeletionDismissed() {
        _uiState.update { it.copy(eventToDelete = null) }
    }


    fun onDeletionConfirmed() {
        val event = _uiState.value.eventToDelete ?: return

        viewModelScope.launch {
            try {

                db.collection("Event").document(event.id!!).delete().await()


                val rsvpQuery = db.collection("event_rspv")
                    .whereEqualTo("eventId", event.id)
                    .limit(1)
                    .get().await()
                sleep(500)
                if (!rsvpQuery.isEmpty) {
                    rsvpQuery.documents[0].reference.delete().await()
                }

            } catch (e: Exception) {

            } finally {

                _uiState.update { it.copy(eventToDelete = null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventsListener?.remove()
    }
}