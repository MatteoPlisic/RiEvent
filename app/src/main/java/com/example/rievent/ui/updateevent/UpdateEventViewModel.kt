package com.example.rievent.ui.updateevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import Event

class UpdateEventViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _event = MutableStateFlow<Event?>(null)
    val event = _event.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun loadEvent(eventId: String) {
        _isLoading.value = true
        db.collection("Event").document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                val event = doc.toObject(Event::class.java)
                if (event != null) {
                    _event.value = event.copy(id = doc.id)
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _errorMessage.value = it.localizedMessage
                _isLoading.value = false
            }
    }

    fun updateEvent(event: Event) {
        if (event.id?.isBlank() == true) {
            _errorMessage.value = "Event ID is missing"
            return
        }

        _isLoading.value = true
        event.id?.let {
            db.collection("Event").document(it)
                .set(event)
                .addOnSuccessListener {
                    _isSuccess.value = true
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _errorMessage.value = it.localizedMessage
                    _isLoading.value = false
                }
        }
    }

    fun resetState() {
        _isSuccess.value = false
        _errorMessage.value = null
    }
}
