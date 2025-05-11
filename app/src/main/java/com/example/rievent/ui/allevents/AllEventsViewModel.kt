package com.example.rievent.ui.allevents

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AllEventsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()

    private val allEvents = mutableListOf<Event>()

    fun loadAllPublicEvents() {
        db.collection("Event")
            .whereEqualTo("public", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val event = doc.toObject(Event::class.java)
                    event?.copy(id = doc.id)
                }
                allEvents.clear()
                allEvents.addAll(list)
                _events.value = list
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

    fun rsvp(id: String?, coming: Any) {


    }
}
