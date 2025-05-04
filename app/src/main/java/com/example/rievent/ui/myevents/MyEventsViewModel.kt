package com.example.rievent.ui.myevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyEventsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()

    fun loadEvents(ownerId: String) {
        listener?.remove()
        listener = db.collection("Event")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val event = doc.toObject(Event::class.java)
                    event?.copy(id = doc.id)
                } ?: emptyList()
                _events.value = list
            }
    }

    fun deleteEvent(eventId: String) {
        db.collection("Event").document(eventId).delete()
    }

    fun updateEvent(event: Event) {

    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
